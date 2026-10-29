package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService

/**
 * Executor for navigation commands.
 *
 * @param service The accessibility service.
 */
class NavigationExecutor(service: AccessibilityService) {
    /**
     * Swipes up.
     */
    fun swipeUp(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Swipes down.
     */
    fun swipeDown(): Boolean {
        return true
    }

    /**
     * Swipes right.
     */
    fun swipeRight(): Boolean {
        return true
    }

    /**
     * Swipes left.
     */
    fun swipeLeft(): Boolean {
        return true
    }

    /**
     * Zooms in.
     */
    fun zoomIn(): Boolean {
        return true
    }

    /**
     * Zooms out.
     */
    fun zoomOut(): Boolean {
        return true
    }

    /**
     * Navigates back.
     */
    fun navigateBack(): Boolean {
        return true
    }

    /**
     * Navigates to home screen.
     */
    fun navigateHome(): Boolean {
        return true
    }

    /**
     * Shows recent apps.
     */
    fun navigateRecent(): Boolean {
        return true
    }

    /**
     * Opens notifications bar.
     */
    fun notifications(): Boolean {
        return true
    }

    /**
     * Opens quick settings.
     */
    fun quickSettings(): Boolean {
        return true
    }

    /**
     * Opens an app.
     */
    fun openApp(): Boolean {
        return true
    }

    /**
     * Locks the screen.
     */
    fun lockScreen(): Boolean {
        return true
    }

    /**
     * Takes a screenshot.
     */
    fun screenshot(): Boolean {
        return true
    }
}