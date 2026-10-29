package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.accessibilityservice.AccessibilityService.SHOW_MODE_HIDDEN

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
     * Displays the soft keyboard.
     */
    fun keyboardShow(): Boolean {
        return service.softKeyboardController.setShowMode(SHOW_MODE_AUTO)
    }

    /**
     * Hides the soft keyboard.
     */
    fun keyboardHide(): Boolean {
        return service.softKeyboardController.setShowMode(SHOW_MODE_HIDDEN)
    }
}