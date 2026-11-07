package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log

/**
 * Handles injection of touch, tap, and drag gestures using the Accessibility API.
 */
class TouchExecutor(private val service: AccessibilityService) {

    /**
     * Emulates a single tap gesture at the specified screen coordinates.
     *
     * @param x X coordinate on screen in pixels.
     * @param y Y coordinate on screen in pixels.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun tap(x: Float, y: Float): Boolean {

        Log.i("TouchExecutor", "Tapping at: (${x}, ${y})")


        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i("TouchExecutor", "Gesture complete")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.i("TouchExecutor", "Gesture complete")
            }
        }, null)
    }

    /**
     * Emulates a swipe / drag gesture between two points on the screen.
     *
     * @param startX Starting X coordinate in pixels.
     * @param startY Starting Y coordinate in pixels.
     * @param endX Ending X coordinate in pixels.
     * @param endY Ending Y coordinate in pixels.
     * @param durationMs Duration of the swipe in milliseconds.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean {
        Log.i("TouchExecutor", "Swiping from ($startX, $startY) to ($endX, $endY)")

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i("TouchExecutor", "Swipe complete")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w("TouchExecutor", "Swipe cancelled")
            }
        }, null)
    }
}