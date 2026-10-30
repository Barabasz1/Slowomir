package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService

/**
 * Handles injection of touch, tap, and drag gestures using the Accessibility API.
 *
 * @param service The accessibility service to use for injection.
 */
class MacroExecutor(private val service: AccessibilityService) {
    /**
     * Records a new macro.
     */
    fun record(): Boolean {
        return true //TODO: implementation needed
    }

    /**
     * Finishes recording a macro.
     */
    fun finish(): Boolean {
        return true //TODO: implementation needed
    }

    /**
     * Plays a recorded macro.
     */
    fun play(): Boolean {
        return true //TODO: implementation needed
    }
}