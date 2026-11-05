package com.zpi.slowomir.overlay.ui.grid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class GridView(
    context: Context,
    var rows: Int,
    var cols: Int,
    var lineOpacity: Int = DEFAULT_LINE_OPACITY,
    var labelOpacity: Int = DEFAULT_LABEL_OPACITY,
    var overlayOpacity: Int = DEFAULT_OVERLAY_OPACITY,
    var lineWidthPx: Float = DEFAULT_LINE_WIDTH_PX,
    var labelTextSizePx: Float = DEFAULT_LABEL_TEXT_SIZE_PX
) : View(context) {

    companion object {
        private const val DIM_ALPHA_FACTOR = 0.1f
        private const val DEFAULT_LINE_OPACITY = 180
        private const val DEFAULT_LABEL_OPACITY = 220
        private const val DEFAULT_OVERLAY_OPACITY = 0
        private const val DEFAULT_LINE_WIDTH_PX = 2f
        private const val DEFAULT_LABEL_TEXT_SIZE_PX = 32f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(lineOpacity, 0 ,0, 0)
        strokeWidth = lineWidthPx
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(labelOpacity, 0, 0, 0)
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(
            (labelOpacity * DIM_ALPHA_FACTOR).toInt(),
            255,
            255,
            255
        )
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(
            (labelOpacity * DIM_ALPHA_FACTOR).toInt(),
            0,
            255,
            0
        )
    }


    private val backgroundPaint = Paint().apply {
        color = Color.argb(overlayOpacity, 0, 0, 0)
    }

    private var selectedCell: Int? = null
    private var parentCell: Int? = null
    private var divisions: List<Pair<Int, Int>> = emptyList()

    fun update(selectedCell: Int?, parentCell: Int?, divisions: List<Pair<Int, Int>>) {
        this.selectedCell = selectedCell
        this.parentCell = parentCell
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


                val paint = if (cell == selectedCell) highlightPaint else
                    if (cell == parentCell || parentCell == null) {
                        textPaint
                    } else {
                        dimPaint
                    }


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