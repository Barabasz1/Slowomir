package com.zpi.slowomir.overlay.ui.common

data class ScreenRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val isEmpty: Boolean get() = width <= 0 || height <= 0
    val area: Long get() = if (isEmpty) 0L else width.toLong() * height

    fun inflate(by: Int) = ScreenRect(left - by, top - by, right + by, bottom + by)

    fun intersectionArea(other: ScreenRect): Long {
        val w = minOf(right, other.right) - maxOf(left, other.left)
        val h = minOf(bottom, other.bottom) - maxOf(top, other.top)
        return if (w <= 0 || h <= 0) 0L else w.toLong() * h
    }
}

enum class ObstacleKind(val weight: Float) {
    FOCUSED_FIELD(100f),
    KEYBOARD(100f),
    INTERACTIVE(1f),
    CONTENT(0.1f),
}

data class Obstacle(
    val bounds: ScreenRect,
    val kind: ObstacleKind,
    val importance: Float = 1f,
)

fun UiSnapshot.toObstacles(): List<Obstacle> {
    val result = nodes.mapTo(mutableListOf()) { node ->
        val kind = when {
            node.focused && node.editable -> ObstacleKind.FOCUSED_FIELD
            node.interactive -> ObstacleKind.INTERACTIVE
            else -> ObstacleKind.CONTENT
        }
        Obstacle(node.bounds, kind)
    }

    focusedField?.takeIf { result.none { it.kind == ObstacleKind.FOCUSED_FIELD } }
        ?.let { result += Obstacle(it, ObstacleKind.FOCUSED_FIELD) }
    keyboard?.let { result += Obstacle(it, ObstacleKind.KEYBOARD) }
    return result
}

fun List<Obstacle>.discountSmall(tinyPx: Int, fullPx: Int, minImportance: Float = 0.2f): List<Obstacle> =
    mapNotNull {
        if (it.kind == ObstacleKind.FOCUSED_FIELD || it.kind == ObstacleKind.KEYBOARD) return@mapNotNull it
        val side = minOf(it.bounds.width, it.bounds.height)
        when {
            side <= tinyPx -> null
            side >= fullPx -> it
            else -> {
                val t = (side - tinyPx).toFloat() / (fullPx - tinyPx)
                it.copy(importance = it.importance * (minImportance + (1f - minImportance) * t))
            }
        }
    }

fun List<Obstacle>.toAvoidByOverlay(tinyPx: Int, fullPx: Int, fieldGapPx: Int): List<Obstacle> =
    discountSmall(tinyPx, fullPx)
        .map { if (it.kind == ObstacleKind.FOCUSED_FIELD) it.copy(bounds = it.bounds.inflate(fieldGapPx)) else it }
