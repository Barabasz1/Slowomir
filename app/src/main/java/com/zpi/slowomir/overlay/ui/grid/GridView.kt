package com.zpi.slowomir.overlay.ui.grid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class GridView(
    context: Context,
    var rows: Int = 10,
    var cols: Int = 6,
    var lineOpacity: Int = 255,
    var labelOpacity: Int = 255,
    var overlayOpacity: Int = 0,
    var lineWidthPx: Float = 2f,
    var labelTextSizePx: Float = 32f,
    var labels: List<String>? = null
) : View(context) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(lineOpacity.coerceIn(0, 255), 255, 255, 255)
        strokeWidth = lineWidthPx
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(labelOpacity.coerceIn(0, 255), 255, 255, 255)
    }
    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(overlayOpacity.coerceIn(0, 255), 0, 0, 0)
    }

    fun configure(
        rows: Int = this.rows,
        cols: Int = this.cols,
        lineOpacity: Int = this.lineOpacity,
        labelOpacity: Int = this.labelOpacity,
        overlayOpacity: Int = this.overlayOpacity,
        lineWidthPx: Float = this.lineWidthPx,
        labelTextSizePx: Float = this.labelTextSizePx,
        labels: List<String>? = this.labels
    ) {
        this.rows = rows.coerceAtLeast(1)
        this.cols = cols.coerceAtLeast(1)
        this.lineOpacity = lineOpacity.coerceIn(0, 255)
        this.labelOpacity = labelOpacity.coerceIn(0, 255)
        this.overlayOpacity = overlayOpacity.coerceIn(0, 255)
        this.lineWidthPx = lineWidthPx.coerceAtLeast(0f)
        this.labelTextSizePx = labelTextSizePx.coerceAtLeast(1f)
        this.labels = labels
        linePaint.color = Color.argb(this.lineOpacity, 255, 255, 255)
        linePaint.strokeWidth = this.lineWidthPx
        textPaint.color = Color.argb(this.labelOpacity, 255, 255, 255)
        textPaint.textSize = this.labelTextSizePx
        backgroundPaint.color = Color.argb(this.overlayOpacity, 0, 0, 0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0 || rows <= 0 || cols <= 0) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        val cellWidth = width.toFloat() / cols
        val cellHeight = height.toFloat() / rows
        for (col in 0..cols) {
            val x = col * cellWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
        }
        for (row in 0..rows) {
            val y = row * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
        }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val index = row * cols + col
                val label = labels?.getOrNull(index) ?: "${index + 1}"
                val centerX = col * cellWidth + cellWidth / 2f
                val centerY = row * cellHeight + cellHeight / 2f
                val baseline = centerY - (textPaint.ascent() + textPaint.descent()) / 2f
                canvas.drawText(label, centerX, baseline, textPaint)
            }
        }
    }
}
