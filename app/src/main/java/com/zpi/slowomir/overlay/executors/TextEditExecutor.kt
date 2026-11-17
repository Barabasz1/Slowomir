package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.accessibilityservice.InputMethod
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.zpi.slowomir.BuildConfig
import com.zpi.slowomir.utils.AccessibilityUtils
import com.zpi.slowomir.utils.EditorState
import com.zpi.slowomir.utils.TextEditEngine

/**
 * Handles text editing and cursor manipulation actions using the Accessibility API.
 *
 * @property service The accessibility service instance.
 * @property touchExecutor The touch executor instance.
 */
class TextEditExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor,
    private val inputConnection: () -> InputMethod.AccessibilityInputConnection? = { null },
) {
    private val engine = TextEditEngine()
    private var lastOperationId = 0L

    val isKeyboardVisible: Boolean
        get() = runCatching {
            service.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }.getOrDefault(false)

    @Volatile
    private var dictatedLength: Int? = null

    val isDictating: Boolean
        get() = dictatedLength != null

    /**
     * Enters text dictation mode. Fails when no text field is connected to the service's input method,
     * i.e. when no editable field has focus. Calling it again starts a fresh session.
     *
     * @return True if dictation mode is on, false otherwise.
     */
    @Synchronized
    fun enter(): Boolean {
        if (inputConnection() == null) {
            Log.w(TAG, "Cannot start dictation: no focused text field.")
            return false
        }
        dictatedLength = 0
        return true
    }

    /**
     * Ends dictation and keeps the text typed in the field.
     *
     * @return True if dictation was on, false if there was nothing to confirm.
     */
    @Synchronized
    fun confirm(): Boolean {
        if (!isDictating) return false
        dictatedLength = null
        return true
    }

    /**
     * Ends dictation and deletes everything typed since [enter].
     *
     * @return True if dictation was on and the text was removed, false otherwise.
     */
    @Synchronized
    fun cancel(): Boolean {
        val length = dictatedLength ?: return false
        dictatedLength = null
        if (length == 0) return true
        val connection = inputConnection() ?: return false
        return runCatching { connection.deleteSurroundingText(length, 0) }
            .onFailure { Log.e(TAG, "deleteSurroundingText failed", it) }
            .isSuccess
    }

    /**
     * Types [text] at the cursor of the focused text field
     *
     * @return True if the text was committed, false if no text field is connected or the text is blank.
     */
    fun insertText(text: String): Boolean {
        if (text.isBlank()) return false

        val connection = inputConnection() ?: run {
            Log.w(TAG, "No input connection: no text field is focused.")
            return false
        }

        val typed = if (needsLeadingSpace(connection)) " $text" else text
        val committed = runCatching { connection.commitText(typed, 1, null) }
            .onFailure { Log.e(TAG, "commitText failed", it) }
            .isSuccess

        if (committed) dictatedLength = dictatedLength?.plus(typed.length)

        return committed
    }

    private fun needsLeadingSpace(connection: InputMethod.AccessibilityInputConnection): Boolean = runCatching {
        val context = connection.getSurroundingText(1, 0, 0) ?: return false
        val cursor = minOf(context.selectionStart, context.selectionEnd)
        val before = context.text.takeIf { cursor in 1..it.length }?.get(cursor - 1)
        before != null && !before.isWhitespace()
    }.getOrDefault(false)

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorForward(): Boolean = applyEngineOperation("TEXT_CURSOR_FORWARD") { engine.cursorForward(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorBackward(): Boolean = applyEngineOperation("TEXT_CURSOR_BACKWARD") { engine.cursorBackward(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorWordNext(): Boolean = applyEngineOperation("TEXT_CURSOR_NEXT_WORD") { engine.cursorWordNext(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorWordPrevious(): Boolean = applyEngineOperation("TEXT_CURSOR_PREVIOUS_WORD") { engine.cursorWordPrevious(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun deleteChar(): Boolean = applyEngineOperation("TEXT_DELETE_CHARACTER") { engine.deleteChar(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun deleteWord(): Boolean = applyEngineOperation("TEXT_DELETE_WORD") { engine.deleteWord(it) }

    /**
     * Applies an engine operation to the active input node.
     *
     * @param operationName A description of the operation being applied.
     * @param operation The operation to apply.
     * @return True if the operation was applied successfully, false otherwise.
     */
    @Synchronized
    private fun applyEngineOperation(
        operationName: String,
        operation: (EditorState) -> EditorState,
    ): Boolean {
        val operationId = ++lastOperationId
        val label = "[$operationName#$operationId]"
        val targetNode = AccessibilityUtils.findActiveInputNode(service) ?: run {
            Log.w(TAG, "$label No active input node found.")
            return false
        }

        if (!targetNode.refresh() || !isActiveEditor(targetNode)) {
            Log.w(TAG, "$label Editor is stale or no longer focused.")
            return false
        }

        // Snapshot the text so later node updates cannot change the engine's input.
        val currentState = readState(targetNode)
        logState(label, "before", currentState)
        if (!currentState.isSelectionValid) {
            Log.w(TAG, "$label Invalid selection; operation skipped.")
            return false
        }

        val newState = operation(currentState)
        val textChanged = newState.textString != currentState.textString
        val selectionChanged = newState.selectionStart != currentState.selectionStart ||
            newState.selectionEnd != currentState.selectionEnd
        logState(label, "target", newState)
        logDebug("$label textChanged=$textChanged, selectionChanged=$selectionChanged")
        if (!textChanged && !selectionChanged) {
            Log.i(TAG, "$label No-op at text boundary.")
            return false
        }

        if (textChanged) {
            if (!supportsAction(targetNode, AccessibilityNodeInfo.ACTION_SET_TEXT)) {
                Log.w(TAG, "$label Editor does not support ACTION_SET_TEXT.")
                return false
            }
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newState.text)
            }
            val accepted = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            logDebug("$label setTextAccepted=$accepted")
            if (!accepted) return false

            // Only observe the same editor; the destructive action is never repeated.
            val afterText = awaitState(targetNode) { it.textString == newState.textString }
            logDebug("$label textVerified=${afterText != null}")
            if (afterText == null) return false
            logState(label, "afterText", afterText)
            if (matchesState(afterText, newState)) return true
        }

        if (!supportsAction(targetNode, AccessibilityNodeInfo.ACTION_SET_SELECTION)) {
            Log.w(TAG, "$label Selection unsupported; textChanged=$textChanged.")
            return false
        }
        val selectionArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newState.selectionStart)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newState.selectionEnd)
        }
        val accepted = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
        logDebug("$label selectionAccepted=$accepted")
        if (!accepted) {
            Log.w(TAG, "$label Selection rejected; textChanged=$textChanged. No text retry.")
            return false
        }

        val actualState = awaitState(targetNode) { matchesState(it, newState) }
        if (actualState != null) logState(label, "afterSelection", actualState)
        logDebug("$label selectionVerified=${actualState != null}")
        return actualState != null
    }

    /**
     * Checks if the given editor state matches the expected state.
     *
     * @param actual The actual editor state.
     * @param expected The expected editor state.
     * @return True if the states match, false otherwise.
     */
    private fun matchesState(actual: EditorState, expected: EditorState): Boolean =
        actual.textString == expected.textString &&
            actual.selectionStart == expected.selectionStart &&
            actual.selectionEnd == expected.selectionEnd

    /**
     * Polls for the effect, not for permission to repeat an action.
     * Legacy direct calls on the main thread only perform an immediate check.
     *
     * @param node The node to poll.
     * @param matches A function that takes an editor state and returns a boolean indicating if the state matches.
     * @return The editor state if it matches, null otherwise.
     */
    private fun awaitState(
        node: AccessibilityNodeInfo,
        matches: (EditorState) -> Boolean,
    ): EditorState? {
        val mainLooper = Looper.getMainLooper()
        val canWait = mainLooper != null && Looper.myLooper() != mainLooper
        val deadline = System.nanoTime() + VERIFICATION_TIMEOUT_MS * 1_000_000
        while (true) {
            if (!node.refresh() || !isActiveEditor(node)) return null
            val state = readState(node)
            if (matches(state)) return state
            if (!canWait || System.nanoTime() >= deadline) return null
            try {
                Thread.sleep(VERIFICATION_POLL_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
        }
    }

    /**
     * Checks if the given node is the active editor.
     *
     * @param node The node to check.
     * @return True if the node is the active editor, false otherwise.
     */
    private fun isActiveEditor(node: AccessibilityNodeInfo): Boolean =
        node.isFocused && node.isEnabled && AccessibilityUtils.isInputCandidate(node)

    /**
     * Checks if the given node supports the given action.
     *
     * @param node The node to check.
     * @param action The action to check for.
     * @return True if the node supports the action, false otherwise.
     */
    private fun supportsAction(node: AccessibilityNodeInfo, action: Int): Boolean =
        node.actionList?.any { it.id == action } == true

    /**
     * Reads the current state of the active editor.
     *
     * @param node The node to read the state from.
     * @return The current editor state.
     */
    private fun readState(node: AccessibilityNodeInfo): EditorState =
        EditorState(node.text?.toString().orEmpty(), node.textSelectionStart, node.textSelectionEnd)

    /**
     * Logs the current state of the active editor.
     *
     * @param label A label to prepend to the log message.
     * @param stage A description of the stage being logged.
     * @param state The current editor state.
     */
    private fun logState(label: String, stage: String, state: EditorState) {
        logDebug("$label ${stage}Len=${state.text.length}, ${stage}Selection=${state.selectionStart}..${state.selectionEnd}")
    }

    /**
     * Logs a debug message if the app is in debug mode.
     *
     * @param message The message to log.
     * @return The message to log.
     */
    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    /**
     * Shows the soft keyboard if it's not already visible.
     *
     * @return True if the keyboard was shown, false otherwise.
     */
    fun keyboardShow(): Boolean {
        ensureAutoShowMode()
        if (isKeyboardVisible) {
            Log.i(TAG, "Soft keyboard is already visible.")
            return true
        }

        val targetNode = AccessibilityUtils.findActiveInputNode(service)
        if (targetNode != null) {
            return touchExecutor.tapNode(targetNode)
        }

        Log.w(TAG, "No focused or editable input node found to show keyboard.")
        return false
    }

    /**
     * Hides the soft keyboard if it's visible.
     *
     * @return True if the keyboard was hidden, false otherwise.
     */
    fun keyboardHide(): Boolean {
        ensureAutoShowMode()
        if (!isKeyboardVisible) {
            Log.i(TAG, "Soft keyboard is already hidden; skipping back action.")
            return true
        }

        // Dismiss soft keyboard window via system BACK action
        return service.performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Toggles the soft keyboard visibility.
     *
     * @return True if the keyboard was shown, false if it was hidden.
     */
    fun keyboardToggle(): Boolean {
        return if (isKeyboardVisible) keyboardHide() else keyboardShow()
    }

    /**
     * Ensures that the soft keyboard is in auto-show mode.
     */
    private fun ensureAutoShowMode() {
        service.softKeyboardController.setShowMode(SHOW_MODE_AUTO)
    }

    companion object {
        private const val TAG = "TextEditExecutor"
        private const val VERIFICATION_TIMEOUT_MS = 350L
        private const val VERIFICATION_POLL_MS = 16L
    }
}
