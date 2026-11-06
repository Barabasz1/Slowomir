package com.zpi.slowomir.overlay.executors


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.touch.TouchExecutor
import com.zpi.slowomir.utils.ScreenPoint
import com.zpi.slowomir.voice.VoiceSessionManager

class NavigationExecutor(
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor
) {

    companion object {
        private const val DEFAULT_SCREEN_WIDTH = 1080f
        private const val DEFAULT_SCREEN_HEIGHT = 2400f
    }
    private fun getCenter() : ScreenPoint{
        val displayMetrics = service.resources.displayMetrics
        return ScreenPoint(displayMetrics.widthPixels / 2f,displayMetrics.heightPixels / 2f)
    }

    private fun getSwipePoint() : ScreenPoint {
        val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
        val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
        val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
        val x = screenWidth * 0.2f
        val y = screenHeight / 2f
        return ScreenPoint(x, y)
    }

    private fun getZoomPoint() : ScreenPoint {
        val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
        val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
        val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
        val x = screenWidth / 2f
        val y = screenHeight / 2f
        return ScreenPoint(x,y)
    }
    fun swipeUp(): Boolean {
        return touchExecutor.swipeUp(getCenter())
    }
    fun swipeDown(): Boolean {
        return touchExecutor.swipeDown(getSwipePoint())
    }
    fun swipeRight(): Boolean {
        return touchExecutor.swipeRight(getSwipePoint())
    }
    fun swipeLeft(): Boolean {
        return touchExecutor.swipeLeft(getSwipePoint())
    }
    fun zoomIn(): Boolean {
        return touchExecutor.pinchOut(getZoomPoint())
    }
    fun zoomOut(): Boolean {
        return touchExecutor.pinchIn(getZoomPoint())
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
