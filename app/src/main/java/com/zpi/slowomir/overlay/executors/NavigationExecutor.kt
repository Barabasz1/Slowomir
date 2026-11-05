package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log

/**
 * Executor for navigation commands.
 *
 * @param service The accessibility service.
 */
class NavigationExecutor(private val service: AccessibilityService) {

    companion object {
        private const val TAG = "NavigationExecutor"
        private const val MOVE_DURATION_MS = 350L
        private const val HOLD_DURATION_MS = 150L
        private const val SWIPE_RATIO = 0.7f
        private const val PINCH_MIN_GAP_RATIO = 0.05f
        private const val PINCH_MAX_GAP_RATIO = 0.25f
    }

    /**
     * Emulates a swipe up gesture starting at the specified screen coordinates without fling inertia.
     *
     * @param x X coordinate on screen in pixels where the swipe begins.
     * @param y Y coordinate on screen in pixels where the swipe begins.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun swipeUp(x: Float, y: Float): Boolean {
        Log.i(TAG, "Swiping up starting at: ($x, $y)")

        val displayMetrics = service.resources.displayMetrics
        val height = displayMetrics.heightPixels.toFloat()

        val swipeDistance = height * SWIPE_RATIO
        val endY = (y - swipeDistance).coerceAtLeast(0f)

        return performSwipeWithHold(x, y, x, endY)
    }

    /**
     * Emulates a swipe down gesture starting at the specified screen coordinates without fling inertia.
     *
     * @param x X coordinate on screen in pixels where the swipe begins.
     * @param y Y coordinate on screen in pixels where the swipe begins.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun swipeDown(x: Float, y: Float): Boolean {
        Log.i(TAG, "Swiping down starting at: ($x, $y)")

        val displayMetrics = service.resources.displayMetrics
        val height = displayMetrics.heightPixels.toFloat()

        val swipeDistance = height * SWIPE_RATIO
        val endY = (y + swipeDistance).coerceAtMost(height)

        return performSwipeWithHold(x, y, x, endY)
    }

    /**
     * Emulates a swipe left gesture starting at the specified screen coordinates without fling inertia.
     *
     * @param x X coordinate on screen in pixels where the swipe begins.
     * @param y Y coordinate on screen in pixels where the swipe begins.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun swipeLeft(x: Float, y: Float): Boolean {
        Log.i(TAG, "Swiping left starting at: ($x, $y)")

        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()

        val swipeDistance = width * SWIPE_RATIO
        val endX = (x - swipeDistance).coerceAtLeast(0f)

        return performSwipeWithHold(x, y, endX, y)
    }

    /**
     * Emulates a swipe right gesture starting at the specified screen coordinates without fling inertia.
     *
     * @param x X coordinate on screen in pixels where the swipe begins.
     * @param y Y coordinate on screen in pixels where the swipe begins.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun swipeRight(x: Float, y: Float): Boolean {
        Log.i(TAG, "Swiping right starting at: ($x, $y)")

        val displayMetrics = service.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()

        val swipeDistance = width * SWIPE_RATIO
        val endX = (x + swipeDistance).coerceAtMost(width)

        return performSwipeWithHold(x, y, endX, y)
    }

    /**
     * Dispatches a linear drag gesture between two points followed by a stationary hold
     * to eliminate fling momentum / list overscroll upon release.
     *
     * @param startX Starting X coordinate in pixels.
     * @param startY Starting Y coordinate in pixels.
     * @param endX Ending X coordinate in pixels.
     * @param endY Ending Y coordinate in pixels.
     * @param moveDuration Movement duration in milliseconds.
     * @param holdDuration Stationary hold duration in milliseconds.
     * @return `true` if the initial gesture was successfully dispatched to the OS.
     */
    private fun performSwipeWithHold(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        moveDuration: Long = MOVE_DURATION_MS,
        holdDuration: Long = HOLD_DURATION_MS
    ): Boolean {
        Log.i(TAG, "Dragging from ($startX, $startY) to ($endX, $endY) with stationary hold")

        val movePath = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val moveStroke = GestureDescription.StrokeDescription(movePath, 0, moveDuration, true)
        val moveGesture = GestureDescription.Builder().addStroke(moveStroke).build()

        val holdPath = Path().apply {
            moveTo(endX, endY)
            lineTo(endX, endY)
        }
        val holdStroke = moveStroke.continueStroke(holdPath, 0, holdDuration, false)
        val holdGesture = GestureDescription.Builder().addStroke(holdStroke).build()

        return service.dispatchGesture(moveGesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Drag movement complete, dispatching stationary hold")
                service.dispatchGesture(holdGesture, object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.i(TAG, "Hold complete, gesture finished with zero velocity")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.w(TAG, "Hold gesture cancelled")
                    }
                }, null)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Drag gesture cancelled")
            }
        }, null)
    }

    /**
     * Emulates a pinch-out (zoom in) gesture centered at the specified coordinates without fling inertia.
     *
     * @param x Center X coordinate on screen in pixels.
     * @param y Center Y coordinate on screen in pixels.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun zoomIn(x: Float, y: Float): Boolean {
        Log.i(TAG, "Zooming in around ($x, $y)")

        val displayMetrics = service.resources.displayMetrics
        val height = displayMetrics.heightPixels.toFloat()

        val minGap = height * PINCH_MIN_GAP_RATIO
        val maxGap = height * PINCH_MAX_GAP_RATIO

        val start1Y = (y - minGap).coerceIn(0f, height)
        val end1Y = (y - maxGap).coerceIn(0f, height)

        val start2Y = (y + minGap).coerceIn(0f, height)
        val end2Y = (y + maxGap).coerceIn(0f, height)

        return performPinchWithHold(
            start1X = x, start1Y = start1Y, end1X = x, end1Y = end1Y,
            start2X = x, start2Y = start2Y, end2X = x, end2Y = end2Y
        )
    }

    /**
     * Emulates a pinch-in (zoom out) gesture centered at the specified coordinates without fling inertia.
     *
     * @param x Center X coordinate on screen in pixels.
     * @param y Center Y coordinate on screen in pixels.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun zoomOut(x: Float, y: Float): Boolean {
        Log.i(TAG, "Zooming out around ($x, $y)")

        val displayMetrics = service.resources.displayMetrics
        val height = displayMetrics.heightPixels.toFloat()

        val minGap = height * PINCH_MIN_GAP_RATIO
        val maxGap = height * PINCH_MAX_GAP_RATIO

        val start1Y = (y - maxGap).coerceIn(0f, height)
        val end1Y = (y - minGap).coerceIn(0f, height)

        val start2Y = (y + maxGap).coerceIn(0f, height)
        val end2Y = (y + minGap).coerceIn(0f, height)

        return performPinchWithHold(
            start1X = x, start1Y = start1Y, end1X = x, end1Y = end1Y,
            start2X = x, start2Y = start2Y, end2X = x, end2Y = end2Y
        )
    }

    /**
     * Dispatches a two-finger pinch gesture followed by a stationary hold on both fingers
     * to eliminate fling momentum / zoom inertia upon release.
     *
     * @param start1X Starting X coordinate of first finger in pixels.
     * @param start1Y Starting Y coordinate of first finger in pixels.
     * @param end1X Ending X coordinate of first finger in pixels.
     * @param end1Y Ending Y coordinate of first finger in pixels.
     * @param start2X Starting X coordinate of second finger in pixels.
     * @param start2Y Starting Y coordinate of second finger in pixels.
     * @param end2X Ending X coordinate of second finger in pixels.
     * @param end2Y Ending Y coordinate of second finger in pixels.
     * @param moveDuration Movement duration in milliseconds.
     * @param holdDuration Stationary hold duration in milliseconds.
     * @return `true` if the initial gesture was successfully dispatched to the OS.
     */
    private fun performPinchWithHold(
        start1X: Float, start1Y: Float, end1X: Float, end1Y: Float,
        start2X: Float, start2Y: Float, end2X: Float, end2Y: Float,
        moveDuration: Long = MOVE_DURATION_MS,
        holdDuration: Long = HOLD_DURATION_MS,
    ): Boolean {
        Log.i(TAG, "Pinching with hold: finger1 ($start1X, $start1Y)->($end1X, $end1Y), finger2 ($start2X, $start2Y)->($end2X, $end2Y)")

        val movePath1 = Path().apply {
            moveTo(start1X, start1Y)
            lineTo(end1X, end1Y)
        }
        val movePath2 = Path().apply {
            moveTo(start2X, start2Y)
            lineTo(end2X, end2Y)
        }

        val moveStroke1 = GestureDescription.StrokeDescription(movePath1, 0, moveDuration, true)
        val moveStroke2 = GestureDescription.StrokeDescription(movePath2, 0, moveDuration, true)

        val moveGesture = GestureDescription.Builder()
            .addStroke(moveStroke1)
            .addStroke(moveStroke2)
            .build()

        val holdPath1 = Path().apply {
            moveTo(end1X, end1Y)
            lineTo(end1X, end1Y)
        }
        val holdPath2 = Path().apply {
            moveTo(end2X, end2Y)
            lineTo(end2X, end2Y)
        }

        val holdStroke1 = moveStroke1.continueStroke(holdPath1, 0, holdDuration, false)
        val holdStroke2 = moveStroke2.continueStroke(holdPath2, 0, holdDuration, false)

        val holdGesture = GestureDescription.Builder()
            .addStroke(holdStroke1)
            .addStroke(holdStroke2)
            .build()

        return service.dispatchGesture(moveGesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Pinch movement complete, dispatching stationary hold")
                service.dispatchGesture(holdGesture, object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.i(TAG, "Pinch complete with zero velocity")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.w(TAG, "Pinch hold cancelled")
                    }
                }, null)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Pinch movement cancelled")
            }
        }, null)
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
