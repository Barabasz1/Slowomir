package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_FORCE
import com.zpi.slowomir.utils.ScreenPoint
import android.content.Intent
import android.util.Log
import com.zpi.slowomir.utils.SwipeDirection

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


    private fun fromFraction(
        xFraction: Float,
        yFraction: Float,
        mirrorX: Boolean,
        mirrorY: Boolean
    ): ScreenPoint {
        val inverted = touchExecutor.invertedControls
        return getFromFraction(
            if (inverted && mirrorX) 1f - xFraction else xFraction,
            if (inverted && mirrorY) 1f - yFraction else yFraction,
        )
    }

    fun swipe(direction: SwipeDirection, force: Int = DEFAULT_SWIPE_FORCE): Boolean {
        val start = when (direction) {
            SwipeDirection.UP -> fromFraction(0.5f, 0.8f, mirrorX = false, mirrorY = true)
            SwipeDirection.DOWN -> fromFraction(0.5f, 0.2f, mirrorX = false, mirrorY = true)
            SwipeDirection.RIGHT -> fromFraction(0.2f, 0.5f, mirrorX = true, mirrorY = false)
            SwipeDirection.LEFT -> fromFraction(0.8f, 0.5f, mirrorX = true, mirrorY = false)
        }
        return touchExecutor.swipe(start, direction, force)
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
     * Opens the launcher's list of all installed apps (app drawer / app library).
     *
     * @return `true` if the launcher handled the request.
     */
    fun navigateAppLibrary(): Boolean {
        return service.performGlobalAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)
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
     * @param packageName The package name of the app to open.
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
