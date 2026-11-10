package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.util.Log
import android.view.accessibility.AccessibilityWindowInfo
import com.zpi.slowomir.utils.AccessibilityUtils

/**
 * Handles text editing and cursor manipulation actions using the Accessibility API.
 *
 * @param service The accessibility service to use for text actions.
 * @param touchExecutor The touch executor used to dispatch gestures (e.g. tap to focus and summon keyboard).
 */
class TextEditExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor,
) {
    /**
     * Checks whether a soft keyboard (Input Method) window is currently visible on the screen.
     */
    val isKeyboardVisible: Boolean
        get() = runCatching {
            service.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        }.getOrDefault(false)

    /**
     * Sends an enter action or line break.
     */
    fun enter(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Moves cursor forward by one character.
     */
    fun cursorForward(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Moves cursor backward by one character.
     */
    fun cursorBackward(): Boolean {
        return true
    }

    /**
     * Moves cursor forward to the next word.
     */
    fun cursorWordNext(): Boolean {
        return true
    }

    /**
     * Moves cursor backward to the previous word.
     */
    fun cursorWordPrevious(): Boolean {
        return true
    }

    /**
     * Deletes the preceding character.
     */
    fun deleteChar(): Boolean {
        return true
    }

    /**
     * Deletes the preceding word.
     */
    fun deleteWord(): Boolean {
        return true
    }

    /**
     * Confirms current text input.
     */
    fun confirm(): Boolean {
        return true
    }

    /**
     * Displays the soft keyboard by locating the active or focused text input node
     * and performing a physical tap gesture at its center coordinates to trigger the IME.
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
     * Hides the soft keyboard for the current input field without locking
     * the system in a persistent SHOW_MODE_HIDDEN policy state.
     *
     * Skips back navigation if the keyboard is already hidden to avoid navigating away from the current screen.
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
     */
    fun keyboardToggle(): Boolean {
        return if (isKeyboardVisible) keyboardHide() else keyboardShow()
    }

    private fun ensureAutoShowMode() {
        service.softKeyboardController.setShowMode(SHOW_MODE_AUTO)
    }

    companion object {
        private const val TAG = "TextEditExecutor"
    }
}
