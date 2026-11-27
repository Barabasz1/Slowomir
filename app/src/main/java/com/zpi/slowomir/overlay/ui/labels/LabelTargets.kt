package com.zpi.slowomir.overlay.ui.labels

import com.zpi.slowomir.overlay.ui.common.ScreenRect
import com.zpi.slowomir.overlay.ui.common.UiSnapshot
import kotlin.math.abs

data class LabelTarget(
    val number: Int,
    val bounds: ScreenRect,
    val texts: List<String> = emptyList(),
)

fun UiSnapshot.toLabelTargets(
    minSidePx: Int,
    rowToleranceRatio: Float = 0.5f,
): List<LabelTarget> {
    val candidates = nodes
        .asSequence()
        .filter { it.interactive && it.enabled }
        .map { it.bounds }
        .filter { minOf(it.width, it.height) >= minSidePx }
        .distinct()
        .sortedBy { it.area }

    val kept = mutableListOf<Pair<ScreenRect, ScreenRect>>()
    for (candidate in candidates) {
        val overlapping = kept.map { it.first }.filter { candidate.intersectionArea(it) > 0 }
        val fitted = if (overlapping.isEmpty()) candidate else fitAround(candidate, overlapping, minSidePx)
        if (fitted != null) kept += fitted to candidate
    }

    val bySize = kept.sortedBy { it.second.area }
    val texts = HashMap<ScreenRect, MutableList<String>>()
    for (node in nodes) {
        val owner = bySize.firstOrNull { it.second.contains(node.bounds) } ?: continue
        val list = texts.getOrPut(owner.second) { mutableListOf() }
        listOfNotNull(node.text, node.description)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in list }
            .forEach { list += it }
    }

    val order = readingOrder(rowToleranceRatio)
    val ordered = kept.sortedWith { a, b -> order.compare(a.first, b.first) }
    return ordered.mapIndexed { index, (rect, original) ->
        LabelTarget(index + 1, rect, texts[original].orEmpty())
    }
}
fun List<LabelTarget>.findByText(query: String): LabelTarget? {
    val wanted = normalizeLabelText(query)
    if (wanted.isEmpty()) return null

    fun score(target: LabelTarget): Int = target.texts.maxOfOrNull { text ->
        val candidate = normalizeLabelText(text)
        when {
            candidate == wanted -> 3
            candidate.startsWith(wanted) -> 2
            candidate.contains(wanted) -> 1
            else -> 0
        }
    } ?: 0

    return filter { score(it) > 0 }
        .sortedWith(compareByDescending<LabelTarget> { score(it) }.thenBy { it.number })
        .firstOrNull()
}

private val NOT_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]+")

private fun normalizeLabelText(text: String): String =
    text.lowercase().replace(NOT_LETTER_OR_DIGIT, " ").trim()

private fun ScreenRect.contains(other: ScreenRect, tolerance: Int = 2) =
    other.left >= left - tolerance && other.top >= top - tolerance &&
        other.right <= right + tolerance && other.bottom <= bottom + tolerance

private fun readingOrder(rowToleranceRatio: Float) = Comparator<ScreenRect> { a, b ->
    val tolerance = (minOf(a.height, b.height) * rowToleranceRatio).toInt()
    if (abs(a.top - b.top) > tolerance) a.top.compareTo(b.top) else a.left.compareTo(b.left)
}

private fun fitAround(rect: ScreenRect, others: List<ScreenRect>, minSidePx: Int): ScreenRect? {
    if (others.size > MAX_JOINT_OTHERS) return others.fold<ScreenRect, ScreenRect?>(rect) { acc, other ->
        acc?.let { if (it.intersectionArea(other) == 0L) it else trimAround(it, other, minSidePx) }
    }

    val xs = (listOf(rect.left, rect.right) + others.flatMap { listOf(it.left, it.right) })
        .filter { it in rect.left..rect.right }.distinct().sorted()
    val ys = (listOf(rect.top, rect.bottom) + others.flatMap { listOf(it.top, it.bottom) })
        .filter { it in rect.top..rect.bottom }.distinct().sorted()

    var best: ScreenRect? = null
    var bestScore = 0.0
    for (l in xs.indices) for (r in l + 1 until xs.size) {
        if (xs[r] - xs[l] < minSidePx) continue
        for (t in ys.indices) for (b in t + 1 until ys.size) {
            if (ys[b] - ys[t] < minSidePx) continue
            val part = ScreenRect(xs[l], ys[t], xs[r], ys[b])
            val score = part.area * (if (part.left == rect.left && part.right == rect.right) FULL_WIDTH_BONUS else 1.0)
            if (score <= bestScore || others.any { part.intersectionArea(it) > 0 }) continue
            best = part
            bestScore = score
        }
    }
    return best
}

private const val MAX_JOINT_OTHERS = 6
private const val FULL_WIDTH_BONUS = 1.3

private fun trimAround(rect: ScreenRect, other: ScreenRect, minSidePx: Int): ScreenRect? {
    fun List<ScreenRect>.largestUsable() =
        filter { minOf(it.width, it.height) >= minSidePx }.maxByOrNull { it.area }

    val vertical = listOf(
        rect.copy(top = maxOf(rect.top, other.bottom)),
        rect.copy(bottom = minOf(rect.bottom, other.top)),
    )
    val horizontal = listOf(
        rect.copy(left = maxOf(rect.left, other.right)),
        rect.copy(right = minOf(rect.right, other.left)),
    )
    val bestVertical = vertical.largestUsable()
    val bestHorizontal = horizontal.largestUsable()
    return when {
        bestVertical == null -> bestHorizontal
        bestHorizontal == null -> bestVertical
        bestVertical.area >= rect.area * PREFERRED_VERTICAL_KEEP -> bestVertical
        else -> maxOf(bestVertical, bestHorizontal, compareBy { it.area })
    }
}

private const val PREFERRED_VERTICAL_KEEP = 0.5f
