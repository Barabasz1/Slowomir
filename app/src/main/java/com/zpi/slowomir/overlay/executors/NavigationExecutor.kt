package com.zpi.slowomir.overlay.executors

import android.R.attr.centerX
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import com.zpi.slowomir.touch.TouchExecutor
import com.zpi.slowomir.utils.ScreenPoint

class NavigationExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor
) {

    private fun getCenter() : ScreenPoint{
        val displayMetrics = service.resources.displayMetrics
        return ScreenPoint(displayMetrics.widthPixels / 2f,displayMetrics.heightPixels / 2f)
    }
    fun swipeUp(): Boolean {
        return touchExecutor.swipeUp(getCenter())
    }
    fun swipeDown(): Boolean {
        return touchExecutor.swipeDown(getCenter())
    }
    fun swipeRight(): Boolean {
        return touchExecutor.swipeRight(getCenter())
    }
    fun swipeLeft(): Boolean {
        return touchExecutor.swipeLeft(getCenter())
    }

    fun zoomIn(): Boolean {
        val center: ScreenPoint  = getCenter()
        return touchExecutor.pinchOut(center.x, center.y)
    }
    fun zoomOut(): Boolean {
        val center: ScreenPoint  = getCenter()
        return touchExecutor.pinchIn(center.x, center.y)
    }

    /**
     * Navigates back.
     *
     * @return `true` if the back navigation was successful.
     */
    fun navigateBack(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Navigates to home screen.
     *
     * @return `true` if the home screen was successfully opened.
     */
    fun navigateHome(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Shows recent apps.
     *
     * @return `true` if the recent apps were successfully opened.
     */
    fun navigateRecent(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Opens notifications bar.
     *
     * @return `true` if the notifications were successfully opened.
     */
    fun notifications(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        // GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE for closing the panel, worth considering
    }

    /**
     * Opens quick settings.
     *
     * @return `true` if the quick settings were successfully opened.
     */
    fun quickSettings(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /**
     * Opens an app.
     *
     * @return `true` if the app was successfully opened.
     */
    fun openApp(): Boolean {
        return true
    }

    /**
     * Locks the screen.
     *
     * @return `true` if the screen was successfully locked.
     */
    fun lockScreen(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    /**
     * Takes a screenshot.
     *
     * @return `true` if the screenshot was successfully taken.
     */
    fun screenshot(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}
