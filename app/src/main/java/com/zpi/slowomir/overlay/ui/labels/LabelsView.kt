package com.zpi.slowomir.overlay.ui.labels

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.View
import com.zpi.slowomir.overlay.ui.grid.GridType
import kotlin.math.abs

/**
 * Draws an outline around every [LabelTarget] with its number in a badge inside the top left corner.
 */
class LabelsView(
    context: Context,
    var gridType: GridType = GridType.DEFAULT,
    private val lineOpacity: Int = 180,
    private val labelOpacity: Int = 220,
    private val lineWidthPx: Float = 2f,
    labelTextSizePx: Float = 32f,
) : View(context) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
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
        strokeCap = Paint.Cap.SQUARE
        color = Color.argb(lineOpacity, 0, 0, 0)
        strokeWidth = lineWidthPx * 3f
    }
    private val contrastInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        color = Color.argb(lineOpacity, 255, 255, 255)
        strokeWidth = lineWidthPx
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb((labelOpacity * 0.75f).toInt(), 20, 20, 20)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = labelTextSizePx
        color = Color.argb(labelOpacity, 255, 255, 255)
    }

    private var targets: List<LabelTarget> = emptyList()
    private var edges: List<Edge> = emptyList()
    private val origin = IntArray(2)

    fun update(targets: List<LabelTarget>, gridType: GridType = this.gridType) {
        if (targets == this.targets && gridType == this.gridType) return
        this.targets = targets
        this.edges = uniqueEdges(targets)
        this.gridType = gridType
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(origin)
        canvas.translate(-origin[0].toFloat(), -origin[1].toFloat())

        when (gridType) {
            GridType.SINGLE -> drawEdges(canvas, linePaint)
            GridType.DASHED -> drawEdges(canvas, dashedPaint)
            GridType.CONTRAST -> {
                drawEdges(canvas, contrastOuterPaint)
                drawEdges(canvas, contrastInnerPaint)
            }
        }
        targets.forEach { drawBadge(canvas, it) }
    }

    private fun drawEdges(canvas: Canvas, paint: Paint) {
        for (e in edges) {
            if (e.horizontal) canvas.drawLine(e.from, e.line, e.to, e.line, paint)
            else canvas.drawLine(e.line, e.from, e.line, e.to, paint)
        }
    }

    private fun drawBadge(canvas: Canvas, target: LabelTarget) {
        val b = target.bounds
        val label = target.number.toString()
        val metrics = textPaint.fontMetrics
        val textHeight = metrics.bottom - metrics.top
        val wanted = maxOf(textPaint.measureText(label), textHeight) / 2f + 10f
        val radius = minOf(wanted, b.width / 2f - 2f, b.height / 2f - 2f)
        if (radius <= 0f) return
        val cx = b.left + radius + 2f
        val cy = b.top + radius + 2f
        canvas.drawCircle(cx, cy, radius, circlePaint)
        canvas.drawText(label, cx, cy - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
    }

    private class Edge(val horizontal: Boolean, val line: Float, val from: Float, val to: Float)

    private companion object {
        const val SAME_LINE_PX = 1.5f

        fun uniqueEdges(targets: List<LabelTarget>): List<Edge> {
            val result = mutableListOf<Edge>()
            for (t in targets) {
                val b = t.bounds
                add(result, true, b.top.toFloat(), b.left.toFloat(), b.right.toFloat())
                add(result, true, b.bottom.toFloat(), b.left.toFloat(), b.right.toFloat())
                add(result, false, b.left.toFloat(), b.top.toFloat(), b.bottom.toFloat())
                add(result, false, b.right.toFloat(), b.top.toFloat(), b.bottom.toFloat())
            }
            return result
        }

        fun add(edges: MutableList<Edge>, horizontal: Boolean, line: Float, from: Float, to: Float) {
            var pieces = listOf(from to to)
            for (e in edges) {
                if (e.horizontal != horizontal || abs(e.line - line) > SAME_LINE_PX) continue
                pieces = pieces.flatMap { (a, b) ->
                    buildList {
                        if (e.from > a) add(a to minOf(b, e.from))
                        if (e.to < b) add(maxOf(a, e.to) to b)
                        if (e.to <= a || e.from >= b) { clear(); add(a to b) }
                    }
                }.filter { it.second > it.first }
            }
            pieces.forEach { (a, b) -> edges += Edge(horizontal, line, a, b) }
        }
    }
}
