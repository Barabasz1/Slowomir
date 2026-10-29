package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService

/**
 * Handles injection of performing a predicted action using the Accessibility API.
 *
 * @param service The accessibility service to use for injection.
 */
class PredictionExecutor(private val service: AccessibilityService) {
    /**
     * Performs a predicted action.
     */
    fun confirm(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Declines a predicted action.
     */
    fun dismiss(): Boolean {
        return true // TODO: implementation needed
    }
}