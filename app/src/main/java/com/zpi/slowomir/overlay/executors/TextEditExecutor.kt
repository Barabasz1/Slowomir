package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.accessibilityservice.InputMethod
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.SurroundingText
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
    private val fieldCapsModes: () -> Int = { DEFAULT_CAPS_MODES },
    private val editorInfo: () -> EditorInfo? = { null },
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
     * Enters text dictation mode. Fails when there is no focused text field: neither connected to the
     * service's input method nor reachable through the accessibility tree. Calling it again starts a fresh session.
     *
     * @return True if dictation mode is on, false otherwise.
     */
    @Synchronized
    fun enter(): Boolean {
        if (inputConnection() == null && !hasFocusedEditorNode()) {
            Log.w(TAG, "Cannot start dictation: no focused text field.")
            return false
        }
        dictatedLength = 0
        return true
    }

    /**
     * Ends dictation and keeps the text typed in the field.
     *
     * @return True if a text session was on, false otherwise.
     */
    @Synchronized
    fun edit(): Boolean {
        if (!isDictating) return false
        dictatedLength = 0
        return true
    }

    /**
     * Ends the text session and keeps the typed text (it is already in the field).
     * Runs enter action if it is not a new line.
     *
     * @return True if the session was on and the field's action, if it has one, ran; false otherwise.
     */
    @Synchronized
    fun confirm(): Boolean {
        if (!isDictating) return false
        dictatedLength = null
        val action = sendingAction() ?: return true
        return performEditorAction(action)
    }

    /** The field's Enter action; null when Enter would only add a new line. */
    private fun sendingAction(): Int? {
        val options = editorInfo()?.imeOptions ?: return null
        if (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return null
        return (options and EditorInfo.IME_MASK_ACTION).takeIf { it in SENDING_ACTIONS }
    }

    private fun performEditorAction(action: Int): Boolean {
        val connection = inputConnection()
        if (connection != null) {
            val done = runCatching { connection.performEditorAction(action) }
                .onFailure { Log.e(TAG, "performEditorAction failed", it) }
                .isSuccess
            if (done) return true
        }
        val node = AccessibilityUtils.findActiveInputNode(service) ?: run {
            Log.w(TAG, "Cannot run the field's action: no focused text field.")
            return false
        }
        return node.refresh() && node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
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

        val connection = inputConnection()

        if (connection != null) {
            val deleted = runCatching { connection.deleteSurroundingText(length, 0) }
                .onFailure { Log.e(TAG, "deleteSurroundingText failed", it) }
                .isSuccess

            if (deleted) return true
        }

        return applyViaNode("DICTATION_CANCEL", lenientCursor = true) { state ->
            val cursor = state.safeSelectionStart
            val from = (cursor - length).coerceAtLeast(0)
            val text = state.text.toString()
            EditorState(text.removeRange(from, cursor), from, from)
        }
    }

    /**
     * Types [text] at the cursor of the focused text field. Spaces and capitalization behave
     * like a keyboard would.
     *
     * @return True if the text was typed, false if no text field is available or the text is blank.
     */
    fun insertText(text: String): Boolean {
        if (text.isBlank()) return false

        val connection = inputConnection()
        if (connection != null) {
            var typedLength = 0
            val result = runCatching {
                val space = needsLeadingSpace(connection)
                if (space) {
                    connection.commitText(" ", 1, null)
                    typedLength += 1
                }

                val requestedCaps = fieldCapsModes()
                val capsMode =
                    if (requestedCaps == 0) 0 else connection.getCursorCapsMode(requestedCaps)
                val capitalize = capsMode != 0
                val typed = applyFirstLetterCase(text, capitalize)
                Log.d(
                    TAG,
                    "[DICTATE] input='$text' leadingSpace=$space capsMode=$capsMode (0x${
                        capsMode.toString(16)
                    }) -> typed='$typed'"
                )

                connection.commitText(typed, 1, null)
                typedLength += typed.length
            }.onFailure { Log.e(TAG, "commitText failed", it) }

            dictatedLength = dictatedLength?.plus(typedLength)

            if (result.isSuccess || typedLength > 0) return result.isSuccess
        }

        var typedLength = 0
        val typed = applyViaNode("DICTATE", lenientCursor = true) { state ->
            val before = state.text.subSequence(0, state.safeSelectionStart)
            val space = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
            val caps = fieldCapsModes()
            val atWordStart = before.isEmpty() || before.last().isWhitespace()
            val capitalize =
                (caps and TextUtils.CAP_MODE_SENTENCES != 0 && startsSentence(before)) ||
                        (caps and TextUtils.CAP_MODE_WORDS != 0 && atWordStart)
            val typed = space + applyFirstLetterCase(text, capitalize)
            typedLength = typed.length
            Log.d(
                TAG,
                "[DICTATE/node] input='$text' capitalizeByText=$capitalize -> typed='$typed'"
            )
            engine.insertText(state, typed)
        }
        if (typed) dictatedLength = dictatedLength?.plus(typedLength)
        return typed
    }

    /**
     * Types a punctuation [mark] right at the cursor any spaces directly before the
     * cursor are removed so the mark attaches to the preceding word.
     *
     * @return True if the mark was typed, false otherwise.
     */
    @Synchronized
    fun insertPunctuation(mark: String): Boolean {
        if (mark.isEmpty()) return false

        val connection = inputConnection()
        if (connection != null) {
            var removed = 0
            val done = runCatching {
                val before = connection.getSurroundingText(PUNCTUATION_LOOKBEHIND, 0, 0)?.text
                removed = before?.let { trailingSpaces(it) } ?: 0
                if (removed > 0) connection.deleteSurroundingText(removed, 0)
                connection.commitText(mark, 1, null)
            }.onFailure { Log.e(TAG, "commitText failed", it) }.isSuccess
            if (done) {
                dictatedLength = dictatedLength?.let { (it - removed + mark.length).coerceAtLeast(0) }
                return true
            }
        }

        var delta = 0
        val typed = applyViaNode("PUNCTUATION", lenientCursor = true) { state ->
            val cursor = state.safeSelectionStart
            val text = state.text.toString()
            val spaces = trailingSpaces(text.substring(0, cursor))
            delta = mark.length - spaces
            engine.insertText(
                EditorState(text.removeRange(cursor - spaces, cursor), cursor - spaces, cursor - spaces),
                mark,
            )
        }
        if (typed) dictatedLength = dictatedLength?.let { (it + delta).coerceAtLeast(0) }
        return typed
    }

    /** Number of whitespace characters (line breaks excluded) at the end of [text]. */
    private fun trailingSpaces(text: CharSequence): Int =
        text.takeLastWhile { it.isWhitespace() && it != '\n' && it != '\r' }.length

    private fun needsLeadingSpace(connection: InputMethod.AccessibilityInputConnection): Boolean =
        runCatching {
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
    fun cursorForward(): Boolean =
        applyEngineOperation("TEXT_CURSOR_FORWARD") { engine.cursorForward(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorBackward(): Boolean =
        applyEngineOperation("TEXT_CURSOR_BACKWARD") { engine.cursorBackward(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorWordNext(): Boolean =
        applyEngineOperation("TEXT_CURSOR_NEXT_WORD") { engine.cursorWordNext(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun cursorWordPrevious(): Boolean =
        applyEngineOperation("TEXT_CURSOR_PREVIOUS_WORD") { engine.cursorWordPrevious(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun deleteChar(): Boolean =
        applyEngineOperation("TEXT_DELETE_CHARACTER") { engine.deleteChar(it) }

    /**
     * Performs a backspace action.
     *
     * @return True if the backspace action was performed, false otherwise.
     */
    fun deleteWord(): Boolean = applyEngineOperation("TEXT_DELETE_WORD") { engine.deleteWord(it) }

    /**
     * Applies an engine operation to the focused text field.
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
        val connection = inputConnection()
        if (connection != null) {
            val label = "[$operationName#${++lastOperationId}]"
            applyViaConnection(label, connection, operation)?.let { return it }
            Log.i(TAG, "$label Input connection unusable, falling back to the accessibility node.")
        }

        return applyViaNode(operationName, lenientCursor = false, operation)
    }

    /**
     * Applies [operation] through the input connection: reads the text around the cursor, computes the change
     * and writes it.
     *
     * @return true/false when the operation was decided, null when the connection cannot be used (nothing was
     * changed) and the caller should fall back.
     */
    private fun applyViaConnection(
        label: String,
        connection: InputMethod.AccessibilityInputConnection,
        operation: (EditorState) -> EditorState,
    ): Boolean? {
        val context = runCatching { connection.getSurroundingText(CONTEXT_CHARS, CONTEXT_CHARS, 0) }
            .onFailure { Log.w(TAG, "$label getSurroundingText failed", it) }
            .getOrNull() ?: run {
            Log.w(TAG, "$label The field returned no surrounding text.")
            return null
        }

        val offset = windowOffset(context)
        if (offset < 0) {
            Log.w(
                TAG,
                "$label Unknown text offset (${context.offset}), textLen=${context.text.length}."
            )
            return null
        }

        val current =
            EditorState(context.text.toString(), context.selectionStart, context.selectionEnd)
        if (!current.isSelectionValid) {
            Log.w(
                TAG,
                "$label Invalid selection ${current.selectionStart}..${current.selectionEnd}, textLen=${current.text.length}."
            )
            return null
        }
        logState(label, "before", current)

        val target = operation(current)
        logState(label, "target", target)
        if (target.textString == current.textString &&
            target.selectionStart == current.selectionStart && target.selectionEnd == current.selectionEnd
        ) {
            Log.i(TAG, "$label No-op at text boundary.")
            return false
        }

        val written = runCatching { writeChange(connection, offset, current, target) }
            .onFailure { Log.e(TAG, "$label Writing through the input connection failed", it) }
            .isSuccess
        if (!written) return false

        val verified = awaitConnectionState(connection, offset, target)
        logDebug("$label connectionVerified=$verified")
        return verified
    }

    /**
     * Where the text of [context] starts in the field.
     */
    private fun windowOffset(context: SurroundingText): Int {
        if (context.offset >= 0) return context.offset
        val cursor = minOf(context.selectionStart, context.selectionEnd)
        return if (cursor in 0 until CONTEXT_CHARS) 0 else -1
    }

    /**
     * Turns the difference between two editor states into input connection calls.
     * Offsets are in the field's text, [offset] is where the window the states describe starts.
     */
    private fun writeChange(
        connection: InputMethod.AccessibilityInputConnection,
        offset: Int,
        current: EditorState,
        target: EditorState,
    ) {
        val old = current.textString
        val new = target.textString

        var prefix = 0
        while (prefix < old.length && prefix < new.length && old[prefix] == new[prefix]) prefix++

        var suffix = 0
        while (suffix < old.length - prefix && suffix < new.length - prefix &&
            old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
        ) suffix++

        val from = prefix
        val to = old.length - suffix
        val inserted = new.substring(prefix, new.length - suffix)

        var selection = current.safeSelectionStart to current.safeSelectionEnd
        fun select(start: Int, end: Int) {
            if (selection == start to end) return
            connection.setSelection(offset + start, offset + end)
            selection = start to end
        }

        if (inserted.isEmpty() && from < to) {
            select(to, to)
            connection.deleteSurroundingText(to - from, 0)
            selection = from to from
        } else if (inserted.isNotEmpty()) {
            select(from, to)
            connection.commitText(inserted, 1, null)
            selection = (from + inserted.length) to (from + inserted.length)
        }

        select(target.safeSelectionStart, target.safeSelectionEnd)
    }

    /**
     * Polls the field through the connection until it shows [expected].
     * An unreadable field counts as verified, since the write itself did not fail.
     */
    private fun awaitConnectionState(
        connection: InputMethod.AccessibilityInputConnection,
        offset: Int,
        expected: EditorState,
    ): Boolean {
        val mainLooper = Looper.getMainLooper()
        val canWait = mainLooper != null && Looper.myLooper() != mainLooper
        val deadline = System.nanoTime() + VERIFICATION_TIMEOUT_MS * 1_000_000
        val expectedText = expected.textString

        while (true) {
            val context = runCatching {
                connection.getSurroundingText(
                    CONTEXT_CHARS,
                    CONTEXT_CHARS,
                    0
                )
            }.getOrNull()
                ?: return true

            val windowStart = windowOffset(context)
            if (windowStart < 0) return true

            val actual = context.text.toString()
            val from = maxOf(windowStart, offset)
            val to = minOf(windowStart + actual.length, offset + expectedText.length)
            val textMatches = to <= from ||
                    actual.substring(from - windowStart, to - windowStart) ==
                    expectedText.substring(from - offset, to - offset)

            val selectionMatches =
                windowStart + minOf(
                    context.selectionStart,
                    context.selectionEnd
                ) == offset + expected.safeSelectionStart &&
                        windowStart + maxOf(
                    context.selectionStart,
                    context.selectionEnd
                ) == offset + expected.safeSelectionEnd

            if (textMatches && selectionMatches) return true
            if (!canWait || System.nanoTime() >= deadline) return false

            try {
                Thread.sleep(VERIFICATION_POLL_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
    }

    /**
     * Applies an engine operation through the accessibility node of the focused editor.
     *
     * @param operationName A description of the operation being applied.
     * @param lenientCursor Typing mode: a field that shows its hint or reports no cursor is treated as having
     * no text (resp. the cursor at its end) instead of being rejected.
     * @param operation The operation to apply.
     *
     * @return True if the operation was applied successfully, false otherwise.
     */
    @Synchronized
    private fun applyViaNode(
        operationName: String,
        lenientCursor: Boolean,
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
        val currentState =
            if (lenientCursor) readStateForTyping(targetNode) else readState(targetNode)
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
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newState.text
                )
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
            putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                newState.selectionStart
            )
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newState.selectionEnd)
        }
        val accepted =
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
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
        hasInputFocus(node) && node.isEnabled && AccessibilityUtils.isInputCandidate(node)

    /**
     * Text fields wrapped in a layout often report focus on the wrapper while the
     * EditText itself says it is not focused, so a few ancestors are checked too.
     */
    private fun hasInputFocus(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(FOCUS_ANCESTOR_DEPTH + 1) {
            if (current?.isFocused == true) return true
            current = current?.parent
        }
        return false
    }

    private fun hasFocusedEditorNode(): Boolean = runCatching {
        val node = AccessibilityUtils.findActiveInputNode(service) ?: return false
        node.refresh() && isActiveEditor(node)
    }.getOrDefault(false)

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
     * Like [readState], but for typing into a field that reports no cursor: an empty field exposes its hint
     * as text with selection -1..-1, so the hint is dropped and the cursor goes to the end of the text.
     */
    private fun readStateForTyping(node: AccessibilityNodeInfo): EditorState {
        val showingHint = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && node.isShowingHintText
        val text = if (showingHint) "" else node.text?.toString().orEmpty()
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val usable = !showingHint && start in 0..text.length && end in 0..text.length
        return if (usable) EditorState(text, start, end) else EditorState(
            text,
            text.length,
            text.length
        )
    }

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

        private val SENDING_ACTIONS = setOf(
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_PREVIOUS,
        )

        private const val CONTEXT_CHARS = 1000
        private const val PUNCTUATION_LOOKBEHIND = 20
        private const val FOCUS_ANCESTOR_DEPTH = 3
        private const val DEFAULT_CAPS_MODES = TextUtils.CAP_MODE_SENTENCES

        internal fun startsSentence(before: CharSequence): Boolean {
            val trimmed = before.trimEnd()
            return trimmed.isEmpty() || trimmed.last() in ".?!\u2026"
        }

        internal fun applyFirstLetterCase(text: String, capitalize: Boolean): String {
            if (text.isEmpty() || !capitalize) return text
            return text.replaceFirstChar { it.uppercaseChar() }
        }

        private const val VERIFICATION_TIMEOUT_MS = 350L
        private const val VERIFICATION_POLL_MS = 16L
    }
}
