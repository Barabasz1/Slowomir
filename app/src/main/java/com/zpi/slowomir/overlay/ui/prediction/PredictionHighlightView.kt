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

    private val targetRect = Rect()
    private val drawRectF = RectF()
    private var isHighlightActive = false

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
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

        val padding = 6f * pulseFactor
        val radius = 16f

        drawRectF.set(
            targetRect.left - padding,
            targetRect.top - padding,
            targetRect.right + padding,
            targetRect.bottom + padding
        )

        canvas.drawRoundRect(drawRectF, radius, radius, fillPaint)
        canvas.drawRoundRect(drawRectF, radius, radius, strokePaint)
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }
}
