package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import com.zpi.slowomir.utils.ScreenPoint
import android.content.Intent
import android.util.Log

/**
 * Executor for navigation commands.
 *
 * @param service The accessibility service.
 */
//TODO use ScreenCoordinatesProvider
class NavigationExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor,
) {
    companion object {
        private const val TAG = "NavigationExecutor"
        private const val DEFAULT_SCREEN_WIDTH = 1080f
        private const val DEFAULT_SCREEN_HEIGHT = 2400f
    }

    fun getCenter(): ScreenPoint {
        val displayMetrics = service.resources.displayMetrics
        return ScreenPoint(displayMetrics.widthPixels / 2f, displayMetrics.heightPixels / 2f)
    }

    fun getFromFraction(xFraction: Float, yFraction: Float): ScreenPoint {
        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
        val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
        return ScreenPoint(screenWidth * xFraction, screenHeight * yFraction)
    }


    fun swipeUp(xFraction: Float = 0.5f, yFraction: Float = 0.8f): Boolean {
        return touchExecutor.swipeUp(getFromFraction(xFraction, yFraction))
    }

    fun swipeDown(xFraction: Float = 0.5f, yFraction: Float = 0.2f): Boolean {
        return touchExecutor.swipeDown(getFromFraction(xFraction, yFraction))
    }

    fun swipeRight(xFraction: Float = 0.2f, yFraction: Float = 0.5f): Boolean {
        return touchExecutor.swipeRight(getFromFraction(xFraction, yFraction))
    }

    fun swipeLeft(xFraction: Float = 0.8f, yFraction: Float = 0.5f): Boolean {
        return touchExecutor.swipeLeft(getFromFraction(xFraction, yFraction))
    }

    fun zoomIn(xFraction: Float = 0.5f, yFraction: Float = 0.5f): Boolean {
        return touchExecutor.pinchOut(getFromFraction(xFraction, yFraction))
    }

    fun zoomOut(xFraction: Float = 0.5f, yFraction: Float = 0.5f): Boolean {
        return touchExecutor.pinchIn(getFromFraction(xFraction, yFraction))
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
    fun openApp(packageName: String): Boolean {
        val launchIntent =
            service.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            service.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
            false
        }
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
