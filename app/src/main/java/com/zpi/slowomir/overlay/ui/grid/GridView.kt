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
    var labelTextSizePx: Float = 32f
) : View(context) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(lineOpacity, 0, 0, 0)
        strokeWidth = lineWidthPx
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(labelOpacity, 255, 255, 255)
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb((labelOpacity * 0.1f).toInt(), 255, 255, 255)
    }

    private val backgroundPaint = Paint().apply {
        color = Color.argb(overlayOpacity, 0, 0, 0)
    }

    private var selectedCell: Int? = null
    private var divisions: List<Pair<Int, Int>> = emptyList()

    fun update(selectedCell: Int?, divisions: List<Pair<Int, Int>>) {
        this.selectedCell = selectedCell
        this.divisions = divisions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val cw = width.toFloat() / cols
        val ch = height.toFloat() / rows

        for (i in 0..cols) {
            canvas.drawLine(i * cw, 0f, i * cw, height.toFloat(), linePaint)
        }

        for (i in 0..rows) {
            canvas.drawLine(0f, i * ch, width.toFloat(), i * ch, linePaint)
        }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cell = row * cols + col + 1
                val x = col * cw
                val y = row * ch
                val paint = if (selectedCell == null || cell == selectedCell) textPaint else dimPaint

                drawLabel(canvas, cell.toString(), x, y, cw, ch, paint)

                if (divisions.firstOrNull()?.first == cell) {
                    drawDivision(canvas, x, y, cw, ch, 0)
                }
            }
        }
    }

    private fun drawDivision(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        level: Int
    ) {
        val division = divisions.getOrNull(level) ?: return
        val n = division.second
        val sw = w / n
        val sh = h / n
        val next = divisions.getOrNull(level + 1)

        for (i in 0..n) {
            canvas.drawLine(x, y + i * sh, x + w, y + i * sh, linePaint)
            canvas.drawLine(x + i * sw, y, x + i * sw, y + h, linePaint)
        }

        for (row in 0 until n) {
            for (col in 0 until n) {
                val cell = row * n + col + 1
                val cx = x + col * sw
                val cy = y + row * sh
                val paint = if (next == null || next.first == cell) textPaint else dimPaint

                drawLabel(canvas, cell.toString(), cx, cy, sw, sh, paint)

                if (next?.first == cell) {
                    drawDivision(canvas, cx, cy, sw, sh, level + 1)
                }
            }
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        label: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        paint: Paint
    ) {
        val baseline = y + h / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(label, x + w / 2f, baseline, paint)
    }
}