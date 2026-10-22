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
}