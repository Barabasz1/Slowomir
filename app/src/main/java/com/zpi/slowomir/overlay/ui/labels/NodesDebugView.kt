package com.zpi.slowomir.overlay.ui.labels

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.zpi.slowomir.overlay.ui.common.UiNode

class NodesDebugView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val tagBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 0, 0)
    }
    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f * density
        color = Color.WHITE
    }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density
    }

    private var nodes: List<UiNode> = emptyList()
    private val origin = IntArray(2)

    fun update(nodes: List<UiNode>) {
        this.nodes = nodes.sortedByDescending { it.bounds.area }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(origin)
        canvas.translate(-origin[0].toFloat(), -origin[1].toFloat())

        val fontMetrics = tagPaint.fontMetrics
        val tagHeight = fontMetrics.descent - fontMetrics.ascent + 2 * density
        val minTaggedSide = 28 * density

        for (node in nodes) {
            val b = node.bounds
            outlinePaint.color = colorOf(node)
            outlinePaint.alpha = if (isInteresting(node)) 230 else 110
            canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), outlinePaint)

            if (minOf(b.width, b.height) < minTaggedSide) continue
            val tag = tagOf(node)
            val tagWidth = minOf(tagPaint.measureText(tag) + 4 * density, b.width.toFloat())
            val left = b.left.toFloat()
            val top = b.top.toFloat()
            canvas.drawRect(left, top, left + tagWidth, top + tagHeight, tagBackgroundPaint)
            canvas.save()
            canvas.clipRect(left, top, left + tagWidth, top + tagHeight)
            canvas.drawText(tag, left + 2 * density, top + 1 * density - fontMetrics.ascent, tagPaint)
            canvas.restore()
        }

        canvas.translate(origin[0].toFloat(), origin[1].toFloat())
        drawLegend(canvas)
    }

    private fun drawLegend(canvas: Canvas) {
        val lineHeight = legendPaint.textSize + 4 * density
        var y = height - LEGEND.size * lineHeight - 16 * density
        val x = 12 * density
        canvas.drawRect(
            x - 6 * density, y - lineHeight, x + 190 * density, y + LEGEND.size * lineHeight - lineHeight + 8 * density,
            tagBackgroundPaint
        )
        for ((color, name) in LEGEND) {
            legendPaint.color = color
            canvas.drawText("■ $name", x, y, legendPaint)
            y += lineHeight
        }
    }

    private fun isInteresting(node: UiNode) =
        node.interactive || node.scrollable || node.focused || !node.enabled

    private fun colorOf(node: UiNode): Int = when {
        !node.enabled -> COLOR_DISABLED
        node.editable -> COLOR_EDITABLE
        node.checkable -> COLOR_CHECKABLE
        node.clickable -> COLOR_CLICKABLE
        node.longClickable -> COLOR_LONG_CLICKABLE
        node.scrollable -> COLOR_SCROLLABLE
        else -> COLOR_PLAIN
    }

    private fun tagOf(node: UiNode): String {
        val flags = buildString {
            if (node.clickable) append('C')
            if (node.longClickable) append('L')
            if (node.checkable) append('K')
            if (node.editable) append('E')
            if (node.scrollable) append('S')
            if (node.focused) append('F')
            if (!node.enabled) append('D')
        }
        val name = node.className?.substringAfterLast('.') ?: "?"
        return if (flags.isEmpty()) name else "$name $flags"
    }

    private companion object {
        val COLOR_CLICKABLE = Color.rgb(76, 175, 80)
        val COLOR_LONG_CLICKABLE = Color.rgb(255, 193, 7)
        val COLOR_CHECKABLE = Color.rgb(255, 87, 34)
        val COLOR_EDITABLE = Color.rgb(233, 30, 99)
        val COLOR_SCROLLABLE = Color.rgb(3, 169, 244)
        val COLOR_DISABLED = Color.rgb(244, 67, 54)
        val COLOR_PLAIN = Color.rgb(158, 158, 158)

        val LEGEND = listOf(
            COLOR_CLICKABLE to "C clickable",
            COLOR_LONG_CLICKABLE to "L long clickable",
            COLOR_CHECKABLE to "K checkable",
            COLOR_EDITABLE to "E editable",
            COLOR_SCROLLABLE to "S scrollable",
            COLOR_DISABLED to "D disabled",
            COLOR_PLAIN to "no interaction",
        )
    }
}
