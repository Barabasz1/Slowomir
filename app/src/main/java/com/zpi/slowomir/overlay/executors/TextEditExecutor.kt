package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService

/**
 *
 *
 * @oaran service
 */
class TextEditExecutor(service: AccessibilityService) {
    /**
     *
     */
    fun enter(): Boolean {
        return true //TODO: implementation needed
    }

    /**
     *
     */
    fun cursorForward(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     *
     */
    fun cursorBackward(): Boolean {
        return true
    }

    /**
     *
     */
    fun cursorWordNext(): Boolean {
        return true
    }

    /**
     *
     */
    fun cursorWordPrevious(): Boolean {
        return true
    }

    /**
     *
     */
    fun deleteChar(): Boolean {
        return true
    }

    /**
     *
     */
    fun deleteWord(): Boolean {
        return true
    }

    /**
     *
     */
    fun confirm(): Boolean {
        return true
    }

    /**
     *
     */
    fun keyboardShow(): Boolean {
        return true
    }

    /**
     *
     */
    fun keyboardHide(): Boolean {
        return true
    }
}