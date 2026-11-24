package com.zpi.slowomir.overlay.ui.prediction

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator

class PredictionHighlightView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val basePaddingPx = 3f * density
    private val cornerRadiusPx = 8f * density

    private val targetRect = Rect()
    private val drawRectF = RectF()
    private val windowLocation = IntArray(2)
    private var isHighlightActive = false

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = 0xFF4CAF50.toInt() // Material Green accent
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x224CAF50 // Semi-transparent green
    }

    private var pulseFactor = 1f
    private val pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.15f).apply {
        duration = 750
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            pulseFactor = it.animatedValue as Float
            invalidate()
        }
    }

    fun setTarget(rect: Rect) {
        targetRect.set(rect)
        isHighlightActive = true
        if (!pulseAnimator.isRunning) {
            pulseAnimator.start()
        }
        invalidate()
    }

    fun clearTarget() {
        isHighlightActive = false
        targetRect.setEmpty()
        pulseAnimator.cancel()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isHighlightActive || targetRect.isEmpty) return

        val padding = basePaddingPx * pulseFactor
        val halfStroke = strokePaint.strokeWidth / 2f

        getLocationOnScreen(windowLocation)
        val offsetX = windowLocation[0].toFloat()
        val offsetY = windowLocation[1].toFloat()

        val left = (targetRect.left - offsetX - padding).coerceAtLeast(halfStroke)
        val top = (targetRect.top - offsetY - padding).coerceAtLeast(halfStroke)
        val right = (targetRect.right - offsetX + padding).coerceAtMost(width - halfStroke)
        val bottom = (targetRect.bottom - offsetY + padding).coerceAtMost(height - halfStroke)

        if (right > left && bottom > top) {
            drawRectF.set(left, top, right, bottom)
            canvas.drawRoundRect(drawRectF, cornerRadiusPx, cornerRadiusPx, fillPaint)
            canvas.drawRoundRect(drawRectF, cornerRadiusPx, cornerRadiusPx, strokePaint)
        }
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }
}
