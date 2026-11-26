package com.zpi.slowomir.overlay.ui.common

import kotlin.math.abs
import kotlin.math.pow

/** Top-left corner of the overlay in screen pixels. */
data class OverlayPosition(val x: Int, val y: Int)

enum class OverlaySide { TOP, BOTTOM }

/**
 * @param position Where the overlay should be.
 * @param side The screen edge it is nearer to.
 * @param offset Distance in pixels from the anchored edge of the window frame to the overlay.
 */
data class OverlayPlacement(val position: OverlayPosition, val side: OverlaySide, val offset: Int)

object SlidingWindowSearch {

    fun search(
        shape: OverlayShape,
        screen: ScreenGeometry,
        obstacles: List<Obstacle>,
        step: Int,
        current: OverlayPosition? = null,
        switchRatio: Float = 0.5f,
        edgeBias: Float = 0.2f,
        coverageExponent: Float = 2f,
    ): OverlayPosition? {
        val width = when (shape) {
            is OverlayShape.Bar -> screen.width
            is OverlayShape.Card -> shape.width
        }
        val height = when (shape) {
            is OverlayShape.Bar -> shape.height
            is OverlayShape.Card -> shape.height
        }

        val minX = 0
        val maxX = screen.width - width
        val minY = screen.topInset
        val maxY = screen.bottomEdge - height
        if (maxX < minX || maxY < minY) return null

        val relevant = obstacles.filter { it.kind != ObstacleKind.KEYBOARD }

        fun coverage(x: Int, y: Int): Float {
            val window = ScreenRect(x, y, x + width, y + height)
            var sum = 0f
            for (o in relevant) {
                val area = o.bounds.area
                if (area > 0) {
                    val fraction = window.intersectionArea(o.bounds).toFloat() / area
                    if (fraction > 0f) sum += o.kind.weight * o.importance * fraction.pow(
                        coverageExponent
                    )
                }
            }
            return sum
        }

        val halfRange = ((maxY - minY) / 2).coerceAtLeast(1)
        fun total(y: Int, coverage: Float) =
            coverage + edgeBias * minOf(y - minY, maxY - y) / halfRange

        val anchor = current ?: OverlayPosition((minX + maxX) / 2, minY)
        val xs = if (shape is OverlayShape.Bar) listOf(minX) else positions(minX, maxX, step)
        var best: OverlayPosition? = null
        var bestCoverage = Float.MAX_VALUE
        var bestTotal = Float.MAX_VALUE
        var bestDistance = Int.MAX_VALUE

        for (y in positions(minY, maxY, step)) {
            for (x in xs) {
                val c = coverage(x, y)
                val t = total(y, c)
                val d = abs(x - anchor.x) + abs(y - anchor.y)
                if (t < bestTotal || (t == bestTotal && d < bestDistance)) {
                    best = OverlayPosition(x, y)
                    bestCoverage = c
                    bestTotal = t
                    bestDistance = d
                }
            }
        }

        val stay =
            current?.let { OverlayPosition(it.x.coerceIn(minX, maxX), it.y.coerceIn(minY, maxY)) }
        if (stay != null) {
            val stayCoverage = coverage(stay.x, stay.y)
            if (stayCoverage <= bestCoverage || bestCoverage >= stayCoverage * switchRatio) return stay
        }
        return best
    }


    fun place(
        shape: OverlayShape,
        screen: ScreenGeometry,
        obstacles: List<Obstacle>,
        step: Int,
        current: OverlayPosition? = null,
    ): OverlayPlacement? {
        val position = search(shape, screen, obstacles, step, current) ?: return null
        val maxY = screen.bottomEdge - shape.height
        val side = if (position.y - screen.topInset <= maxY - position.y) OverlaySide.TOP else OverlaySide.BOTTOM
        val offset = when (side) {
            OverlaySide.TOP -> position.y - screen.topInset
            OverlaySide.BOTTOM -> screen.height - (position.y + shape.height)
        }
        return OverlayPlacement(position, side, offset.coerceAtLeast(0))
    }

    private fun positions(from: Int, to: Int, step: Int): List<Int> {
        val result = generateSequence(from) { it + step }.takeWhile { it < to }.toMutableList()
        result += to
        return result
    }
}
