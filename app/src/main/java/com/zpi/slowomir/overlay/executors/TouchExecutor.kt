package com.zpi.slowomir.touch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.utils.ScreenPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles injection of touch, tap, and drag gestures using the Accessibility API,
 * as well as semantic and coordinate-based clicking of accessibility nodes.
 */
class TouchExecutor(private val service: AccessibilityService) {
@Singleton
class TouchExecutor @Inject constructor() {

    /**
     * Emulates a single tap gesture at the specified screen coordinates.
     *
     * @param x X coordinate on screen in pixels.
     * @param y Y coordinate on screen in pixels.
     * @return `true` if the gesture was successfully dispatched to the OS.
     */
    fun tapAt(x: Float, y: Float): Boolean {
        Log.i(TAG, "Tapping at: ($x, $y)")
    private var service: AccessibilityService? = null

    fun attachService(service: AccessibilityService) {
        this.service = service
    }

    fun detachService() {
        this.service = null
    }

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
    private val defaultStartTime: Long = 0

    fun doGesture(gesture: GestureDescription): Boolean {
        val currentService = service ?: return false
        return currentService.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                }
            },
            null
        )
    }

    private fun getStroke(path: Path, duration: Long, startTime: Long): GestureDescription.StrokeDescription {
        return GestureDescription.StrokeDescription(path, startTime, duration)
    }

    private fun tap(x: Float, y: Float, duration: Long, startTime: Long = defaultStartTime): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = getStroke(path, duration, startTime)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return doGesture(gesture)
    }

    fun shortTap(x: Float, y: Float, startTime: Long = defaultStartTime): Boolean {
        return tap(x, y, 50, startTime)
    }

    fun longTap(x: Float, y: Float, duration: Long = 1000, startTime: Long = defaultStartTime): Boolean {
        return tap(x, y, duration, startTime)
    }

    fun swipe(startX: Float, startY: Float, destX: Float, destY: Float, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(destX, destY)
        }
        val stroke = getStroke(path, duration, startTime)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return doGesture(gesture)
    }

    fun swipeUp(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipe(startX, startY, startX, startY - distance, duration, startTime)
    }

    fun swipeUp(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipeUp(point.x, point.y, distance, duration, startTime)
    }

    fun swipeDown(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipe(startX, startY, startX, startY + distance, duration, startTime)
    }

    fun swipeDown(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipeDown(point.x, point.y, distance, duration, startTime)
    }

    fun swipeRight(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipe(startX, startY, startX + distance, startY, duration, startTime)
    }

    fun swipeRight(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipeRight(point.x, point.y, distance, duration, startTime)
    }

    fun swipeLeft(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipe(startX, startY, startX - distance, startY, duration, startTime)
    }

    fun swipeLeft(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = defaultStartTime): Boolean {
        return swipeLeft(point.x, point.y, distance, duration, startTime)
    }

    private fun pinch(
        startX1: Float, startY1: Float,
        destX1: Float, destY1: Float,
        startX2: Float, startY2: Float,
        destX2: Float, destY2: Float,
        duration: Long = 400, startTime: Long = defaultStartTime
    ): Boolean {
        val path1 = Path().apply {
            moveTo(startX1, startY1)
            lineTo(destX1, destY1)
        }
        val path2 = Path().apply {
            moveTo(startX2, startY2)
            lineTo(destX2, destY2)
        }
        val stroke1 = getStroke(path1, duration, startTime)
        val stroke2 = getStroke(path2, duration, startTime)
        val gesture = GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2).build()
        return doGesture(gesture)
    }

    fun pinchIn(
        centerX: Float,
        centerY: Float,
        power: Float = 1f,
        duration: Long = 400,
        startTime: Long = defaultStartTime
    ): Boolean {
        val distance = 300f * power
        return pinch(
            startX1 = centerX - distance, startY1 = centerY - distance,
            destX1 = centerX - distance / 4f, destY1 = centerY - distance / 4f,
            startX2 = centerX + distance, startY2 = centerY + distance,
            destX2 = centerX + distance / 4f, destY2 = centerY + distance / 4f,
            duration = duration,
            startTime = startTime
        )
    }

    fun pinchOut(
        centerX: Float,
        centerY: Float,
        power: Float = 1f,
        duration: Long = 400,
        startTime: Long = defaultStartTime
    ): Boolean {
        val distance = 300f * power
        return pinch(
            startX1 = centerX - distance / 4f, startY1 = centerY - distance / 4f,
            destX1 = centerX - distance, destY1 = centerY - distance,
            startX2 = centerX + distance / 4f, startY2 = centerY + distance / 4f,
            destX2 = centerX + distance, destY2 = centerY + distance,
            duration = duration,
            startTime = startTime
        )
    }
}
