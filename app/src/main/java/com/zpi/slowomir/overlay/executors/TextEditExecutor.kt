package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Handles text editing and cursor manipulation actions using the Accessibility API.
 *
 * @param service The accessibility service to use for text actions.
 */
class TextEditExecutor(private val service: AccessibilityService) {
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
     * Displays the soft keyboard and re-establishes input focus on the active field.
     */
    fun keyboardShow(): Boolean {
        val showModeSet = service.softKeyboardController.setShowMode(SHOW_MODE_AUTO)

        val focusedNode = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

        return showModeSet
    }

    /**
     * Hides the soft keyboard for the current input field without locking
     * the system in a persistent SHOW_MODE_HIDDEN policy state.
     */
    fun keyboardHide(): Boolean {
        // Ensure system show mode remains AUTO so future focus/typing is not globally blocked
        service.softKeyboardController.setShowMode(SHOW_MODE_AUTO)

        // Dismiss soft keyboard window via system BACK action
        return service.performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
