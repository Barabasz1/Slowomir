package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT

/**
 * Executor for navigation commands.
 *
 * @param service The accessibility service.
 */
class NavigationExecutor(private val service: AccessibilityService) {
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
        return service.performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Navigates to home screen.
     */
    fun navigateHome(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Shows recent apps.
     */
    fun navigateRecent(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Opens notifications bar.
     */
    fun notifications(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        // GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE for closing the panel, worth considering
    }

    /**
     * Opens quick settings.
     */
    fun quickSettings(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
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
        return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    /**
     * Takes a screenshot.
     */
    fun screenshot(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}