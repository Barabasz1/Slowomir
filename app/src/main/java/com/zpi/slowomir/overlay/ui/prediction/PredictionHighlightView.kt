package com.zpi.slowomir.overlay.ui.prediction

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.roundToInt

class PredictionHighlightView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val basePaddingPx = 3f * density
    private val enterOffsetPx = 10f * density
    private val rippleStartPaddingPx = 2f * density
    private val rippleEndPaddingPx = 16f * density
    private val cornerRadiusPx = 10f * density
    private val reenterThresholdPx = (16f * density).roundToInt()

    private val targetRect = Rect()
    private val drawRectF = RectF()
    private val rippleRectF = RectF()
    private val gradientMatrix = Matrix()
    private val windowLocation = IntArray(2)
    private var isHighlightActive = false

    private val brandGradient = LinearGradient(
        0f,
        1f,
        1f,
        0f,
        BRAND_BLUE,
        BRAND_TEAL,
        Shader.TileMode.CLAMP,
    )

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = BRAND_TEAL
        shader = brandGradient
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = BRAND_FILL
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = BRAND_TEAL
    }

    private var enterProgress = 1f
    private val enterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 480
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            enterProgress = it.animatedValue as Float
            invalidate()
        }
    }

    private var pulseFactor = 1f
    private val pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.15f).apply {
        duration = 1000
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            pulseFactor = it.animatedValue as Float
            invalidate()
        }
    }

    fun setTarget(rect: Rect) {
        val wasActive = isHighlightActive
        val shiftedSignificantly = wasActive && (
            abs(targetRect.left - rect.left) > reenterThresholdPx ||
                abs(targetRect.top - rect.top) > reenterThresholdPx ||
                abs(targetRect.right - rect.right) > reenterThresholdPx ||
                abs(targetRect.bottom - rect.bottom) > reenterThresholdPx
            )
        targetRect.set(rect)
        isHighlightActive = true

        if (!wasActive || shiftedSignificantly) {
            enterAnimator.cancel()
            enterProgress = 0f
            enterAnimator.start()
        }
        if (!pulseAnimator.isRunning) {
            pulseAnimator.start()
        }
        invalidate()
    }

    fun clearTarget() {
        isHighlightActive = false
        targetRect.setEmpty()
        enterAnimator.cancel()
        pulseAnimator.cancel()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isHighlightActive || targetRect.isEmpty) return

        val settleOffset = (1f - enterProgress) * enterOffsetPx
        val padding = basePaddingPx * pulseFactor + settleOffset
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
            gradientMatrix.setScale(drawRectF.width(), drawRectF.height())
            gradientMatrix.postTranslate(drawRectF.left, drawRectF.top)
            brandGradient.setLocalMatrix(gradientMatrix)

            if (enterProgress < 1f) {
                val rippleExpand =
                    rippleStartPaddingPx + enterProgress * (rippleEndPaddingPx - rippleStartPaddingPx)
                val rippleHalfStroke = ripplePaint.strokeWidth / 2f
                val rLeft = (drawRectF.left - rippleExpand).coerceAtLeast(rippleHalfStroke)
                val rTop = (drawRectF.top - rippleExpand).coerceAtLeast(rippleHalfStroke)
                val rRight = (drawRectF.right + rippleExpand).coerceAtMost(width - rippleHalfStroke)
                val rBottom = (drawRectF.bottom + rippleExpand).coerceAtMost(height - rippleHalfStroke)
                if (rRight > rLeft && rBottom > rTop) {
                    rippleRectF.set(rLeft, rTop, rRight, rBottom)
                    ripplePaint.alpha = (255f * 0.55f * (1f - enterProgress)).roundToInt().coerceIn(0, 255)
                    val rippleRadius = cornerRadiusPx + rippleExpand * 0.35f
                    canvas.drawRoundRect(rippleRectF, rippleRadius, rippleRadius, ripplePaint)
                }
            }

            val pulseNormalized = ((pulseFactor - 0.85f) / 0.30f).coerceIn(0f, 1f)
            fillPaint.alpha = (BASE_FILL_ALPHA * enterProgress).roundToInt().coerceIn(0, BASE_FILL_ALPHA)
            strokePaint.alpha = ((220f + 35f * pulseNormalized) * enterProgress).roundToInt().coerceIn(0, 255)

            canvas.drawRoundRect(drawRectF, cornerRadiusPx, cornerRadiusPx, fillPaint)
            canvas.drawRoundRect(drawRectF, cornerRadiusPx, cornerRadiusPx, strokePaint)
        }
    }

    override fun onDetachedFromWindow() {
        enterAnimator.cancel()
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        val BRAND_BLUE = 0xFF1F7BF0.toInt()
        val BRAND_TEAL = 0xFF2DD4C8.toInt()
        const val BASE_FILL_ALPHA = 0x24
        val BRAND_FILL = (BASE_FILL_ALPHA shl 24) or 0x2DD4C8
    }
}
