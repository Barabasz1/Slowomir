package com.zpi.slowomir.overlay.ui.grid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
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
    var labelTextSizePx: Float = DEFAULT_LABEL_TEXT_SIZE_PX,
    var gridType: GridType = GridType.DEFAULT
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
        color = Color.argb(lineOpacity, 255, 255, 0)
        strokeWidth = lineWidthPx
    }

    private val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(lineOpacity, 255, 255, 255)
        strokeWidth = lineWidthPx
        pathEffect = DashPathEffect(floatArrayOf(lineWidthPx * 6f, lineWidthPx * 4f), 0f)
    }

    private val contrastOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(lineOpacity, 0, 0, 0)
        strokeWidth = lineWidthPx * 3f
    }

    private val contrastInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(lineOpacity, 255, 255, 255)
        strokeWidth = lineWidthPx
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(labelOpacity, 255, 255, 0)
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(
            (labelOpacity * DIM_ALPHA_FACTOR).toInt(),
            255,
            255,
            0
        )
    }

    private val selectedOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = lineWidthPx * 3f
    }

    private val backgroundPaint = Paint().apply {
        color = Color.argb(overlayOpacity, 0, 0, 0)
    }

    private var selectedCell: Int? = null
    private var parentCell: Int? = null
    private var divisions: List<Pair<Int, Int>> = emptyList()

    fun update(
        rowCount: Int,
        colCount: Int,
        selectedCell: Int?,
        parentCell: Int?,
        divisions: List<Pair<Int, Int>>,
        gridType: GridType = this.gridType
    ) {
        this.gridType = gridType
        this.rows = rowCount
        this.cols = colCount
        this.selectedCell = selectedCell
        this.parentCell = parentCell
        this.divisions = divisions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val lineColor = when (gridType) {
            GridType.SINGLE -> Color.argb(labelOpacity, 255, 255, 0)
            GridType.DASHED -> Color.argb(labelOpacity, 255, 255, 255)
            GridType.CONTRAST -> Color.argb(labelOpacity, 255, 255, 255)
        }
        textPaint.color = lineColor
        dimPaint.color = Color.argb(
            (Color.alpha(lineColor) * DIM_ALPHA_FACTOR).toInt(),
            Color.red(lineColor),
            Color.green(lineColor),
            Color.blue(lineColor)
        )

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val cw = width.toFloat() / cols
        val ch = height.toFloat() / rows

        for (i in 0..cols) {
            drawGridLine(canvas, i * cw, 0f, i * cw, height.toFloat())
        }

        for (i in 0..rows) {
            drawGridLine(canvas, 0f, i * ch, width.toFloat(), i * ch)
        }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val cell = row * cols + col + 1
                val x = col * cw
                val y = row * ch

                val isDivided = divisions.firstOrNull()?.first == cell
                val isSelected = divisions.isEmpty() && cell == selectedCell
                val paint = if (parentCell == null || isSelected) {
                    textPaint
                } else {
                    dimPaint
                }

                if (!isDivided) {
                    drawLabel(canvas, cell.toString(), x, y, cw, ch, paint)
                }

                if (isSelected) {
                    drawCellHighlight(canvas, x, y, cw, ch)
                }

                if (isDivided) {
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
            drawGridLine(canvas, x, y + i * sh, x + w, y + i * sh)
            drawGridLine(canvas, x + i * sw, y, x + i * sw, y + h)
        }

        val isActiveLevel = level == divisions.size - 1

        for (row in 0 until n) {
            for (col in 0 until n) {
                val cell = row * n + col + 1
                val cx = x + col * sw
                val cy = y + row * sh

                val isSubDivided = next?.first == cell
                val isSelected = isActiveLevel && cell == selectedCell
                val paint = if (next == null || isSelected) textPaint else dimPaint

                if (!isSubDivided) {
                    drawLabel(canvas, cell.toString(), cx, cy, sw, sh, paint)
                }

                if (isSelected) {
                    drawCellHighlight(canvas, cx, cy, sw, sh)
                }

                if (isSubDivided) {
                    drawDivision(canvas, cx, cy, sw, sh, level + 1)
                }
            }
        }
    }

    private fun drawGridLine(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        when (gridType) {
            GridType.SINGLE -> canvas.drawLine(x1, y1, x2, y2, linePaint)
            GridType.DASHED -> canvas.drawLine(x1, y1, x2, y2, dashedPaint)
            GridType.CONTRAST -> {
                canvas.drawLine(x1, y1, x2, y2, contrastOuterPaint)
                canvas.drawLine(x1, y1, x2, y2, contrastInnerPaint)
            }
        }
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val whiteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.WHITE
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
        val centerX = x + w / 2f
        val centerY = y + h / 2f
        val alpha = paint.alpha

        whiteTextPaint.textSize = paint.textSize
        val textWidth = whiteTextPaint.measureText(label)
        val fontMetrics = whiteTextPaint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top

        val radius = (maxOf(textWidth, textHeight) / 2f + 10f).coerceAtMost(minOf(w, h) / 2f - 2f)

        if (radius > 0f) {
            circlePaint.color = Color.argb((alpha * 0.75f).toInt(), 20, 20, 20)
            canvas.drawCircle(centerX, centerY, radius, circlePaint)
        }

        whiteTextPaint.color = Color.argb(alpha, 255, 255, 255)
        val baseline = centerY - (whiteTextPaint.ascent() + whiteTextPaint.descent()) / 2f
        canvas.drawText(label, centerX, baseline, whiteTextPaint)
    }

    private fun drawCellHighlight(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float
    ) {
        val halfStroke = selectedOutlinePaint.strokeWidth / 2f
        canvas.drawRect(
            x + halfStroke,
            y + halfStroke,
            x + w - halfStroke,
            y + h - halfStroke,
            selectedOutlinePaint
        )
    }
}