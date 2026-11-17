package com.zpi.slowomir.ml

import kotlin.math.exp

fun softmax(logits: FloatArray): FloatArray {
    val max = logits.max()
    val exps = FloatArray(logits.size) { exp(logits[it] - max) }
    val sum = exps.sum()
    return FloatArray(exps.size) { exps[it] / sum }
}

fun argmax(values: FloatArray): Int = values.indices.maxByOrNull { values[it] } ?: -1

data class LabeledSpan(val label: String, val range: IntRange)

fun isWordStart(text: String, range: IntRange): Boolean =
    range.first == 0 || text[range.first - 1].isWhitespace()

fun decodeBioSpans(
    text: String,
    encoding: TokenEncoding,
    tokenLogits: Array<FloatArray>,
    tags: List<String>,
): List<LabeledSpan> {
    val spans = mutableListOf<LabeledSpan>()
    var label: String? = null
    var start = -1
    var end = -1

    fun flush() {
        label?.let { if (start >= 0) spans += LabeledSpan(it, start until end) }
        label = null; start = -1; end = -1
    }

    for (i in encoding.offsets.indices) {
        val range = encoding.offsets[i] ?: continue
        if (!isWordStart(text, range)) continue
        val row = tokenLogits.getOrNull(i) ?: continue

        var wordEnd = range.last + 1
        while (wordEnd < text.length && !text[wordEnd].isWhitespace()) wordEnd++

        val tag = tags.getOrNull(argmax(row)) ?: "O"
        val type = tag.substringAfter('-', "")

        when {
            tag.startsWith("I-") && label == type -> end = wordEnd
            tag.startsWith("B-") || tag.startsWith("I-") -> {
                flush()
                label = type; start = range.first; end = wordEnd
            }
            else -> flush()
        }
    }
    flush()
    return spans
}
