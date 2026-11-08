package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Handles injection of touch, tap, and drag gestures using the Accessibility API,
 * as well as semantic and coordinate-based clicking of accessibility nodes.
 */
class TouchExecutor(private val service: AccessibilityService) {

    /**
     * Emulates a single tap gesture at the specified screen coordinates.
     *
     * @param x X coordinate on screen in pixels.
     * @param y Y coordinate on screen in pixels.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun tapAt(x: Float, y: Float): Boolean {
        Log.i(TAG, "Tapping at: ($x, $y)")

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.i(TAG, "Gesture complete")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.i(TAG, "Gesture complete")
                }
            },
            null,
        )
    }

    /**
     * Performs a click or tap on the specified [AccessibilityNodeInfo].
     *
     * First attempts to perform a semantic `ACTION_CLICK` on the node or its clickable ancestors.
     * If semantic click is unsupported or fails, falls back to a coordinate-based [tapAt] gesture
     * at the center coordinates of the node's bounds.
     *
     * @param node The [AccessibilityNodeInfo] to interact with.
     * @return `true` if the interaction was successfully performed or dispatched, `false` otherwise.
     */
    fun tapNode(node: AccessibilityNodeInfo): Boolean {
        // 1. Try performAction(ACTION_CLICK) on node or its clickable parents
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            val hasClickAction = current.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
            if ((current.isClickable || hasClickAction) && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = current.parent
        }

        // 2. Fallback to tap gesture at center bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val hasValidBounds = !bounds.isEmpty && bounds.centerX() > 0 && bounds.centerY() > 0
        return hasValidBounds && tapAt(bounds.centerX().toFloat(), bounds.centerY().toFloat())
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
        Log.i(TAG, "Swiping from ($startX, $startY) to ($endX, $endY)")

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "Swipe complete")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Swipe cancelled")
            }
        }, null)
    }

    companion object {
        private const val TAG = "TouchExecutor"
    }
}
