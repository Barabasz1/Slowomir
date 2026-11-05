package com.zpi.slowomir.touch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import com.zpi.slowomir.utils.ScreenPoint
import javax.inject.Inject
import javax.inject.Singleton

class TouchExecutor(
    private val service: AccessibilityService
) {
    companion object {
        private const val TAG = "TouchExecutor"
        private const val DEFAULT_START_TIME: Long = 100

        private const val MOVE_DURATION_MS = 350L
        private const val HOLD_DURATION_MS = 150L
    }

    fun doGesture(gesture: GestureDescription): Boolean {
        val currentService = service
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

    private fun tap(x: Float, y: Float, duration: Long, startTime: Long = DEFAULT_START_TIME): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = getStroke(path, duration, startTime)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return doGesture(gesture)
    }

    fun shortTap(x: Float, y: Float, startTime: Long = DEFAULT_START_TIME): Boolean {
        return tap(x, y, 50, startTime)
    }
    fun shortTap(screenPoint: ScreenPoint, startTime: Long = DEFAULT_START_TIME): Boolean {
        return tap(screenPoint.x, screenPoint.y, 50, startTime)
    }

    fun longTap(x: Float, y: Float, duration: Long = 1000, startTime: Long = DEFAULT_START_TIME): Boolean {
        return tap(x, y, duration, startTime)
    }
    fun longTap(screenPoint: ScreenPoint, duration: Long = 1000, startTime: Long = DEFAULT_START_TIME): Boolean {
        return tap(screenPoint.x,screenPoint.y, duration, startTime)
    }

    fun swipe(startX: Float, startY: Float, destX: Float, destY: Float, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(destX, destY)
        }
        val stroke = getStroke(path, duration, startTime)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return doGesture(gesture)
    }

    fun swipeUp(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipe(startX, startY, startX, startY - distance, duration, startTime)
    }

    fun swipeUp(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipeUp(point.x, point.y, distance, duration, startTime)
    }

    fun swipeDown(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipe(startX, startY, startX, startY + distance, duration, startTime)
    }

    fun swipeDown(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipeDown(point.x, point.y, distance, duration, startTime)
    }

    fun swipeRight(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipe(startX, startY, startX + distance, startY, duration, startTime)
    }

    fun swipeRight(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipeRight(point.x, point.y, distance, duration, startTime)
    }

    fun swipeLeft(startX: Float, startY: Float, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipe(startX, startY, startX - distance, startY, duration, startTime)
    }

    fun swipeLeft(point: ScreenPoint, distance: Float = 500f, duration: Long = 300, startTime: Long = DEFAULT_START_TIME): Boolean {
        return swipeLeft(point.x, point.y, distance, duration, startTime)
    }

    private fun pinch(
        startX1: Float, startY1: Float,
        destX1: Float, destY1: Float,
        startX2: Float, startY2: Float,
        destX2: Float, destY2: Float,
        duration: Long = 400, startTime: Long = DEFAULT_START_TIME
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
    private fun pinch(
        start1: ScreenPoint,
        dest1: ScreenPoint,
        start2: ScreenPoint,
        dest2: ScreenPoint,
        duration: Long = 400, startTime: Long = DEFAULT_START_TIME
    ): Boolean {
        return pinch(
            start1.x, start1.y,
            dest1.x, dest1.y,
            start2.x, start2.y,
            dest2.x, dest2.y,
            duration, startTime)
    }

    fun pinchIn(
        centerX: Float,
        centerY: Float,
        power: Float = 1f,
        duration: Long = 400,
        startTime: Long = DEFAULT_START_TIME
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
    fun pinchIn(
        center: ScreenPoint,
        power: Float = 1f,
        duration: Long = 400,
        startTime: Long = DEFAULT_START_TIME
    ): Boolean {
        return pinchIn(center.x, center.y, power, duration, startTime)
    }

    fun pinchOut(
        centerX: Float,
        centerY: Float,
        power: Float = 1f,
        duration: Long = 400,
        startTime: Long = DEFAULT_START_TIME
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
    fun pinchOut(
        center: ScreenPoint,
        power: Float = 1f,
        duration: Long = 400,
        startTime: Long = DEFAULT_START_TIME
    ): Boolean {
        return pinchOut(center.x, center.y, power, duration, startTime)
    }



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
}
