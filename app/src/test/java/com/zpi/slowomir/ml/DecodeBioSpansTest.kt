package com.zpi.slowomir.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class DecodeBioSpansTest {

    private val tags = listOf("B-NAME", "B-NUMBER", "I-NAME", "I-NUMBER", "O")

    private fun row(tag: String) = FloatArray(tags.size) { if (tags[it] == tag) 10f else 0f }

    @Test
    fun `only the first piece of a word is read`() {
        // "otwórz spotify" = o|twór|z  spot|if|y ; continuation pieces carry arbitrary tags
        val text = "otwórz spotify"
        val enc = TokenEncoding(
            longArrayOf(0, 41, 5217, 135, 40355, 8571, 20, 2),
            listOf(null, 0..0, 1..3, 4..5, 7..10, 11..12, 13..13, null),
        )
        val logits = arrayOf(
            row("B-NAME"), row("O"), row("B-NAME"), row("O"),
            row("B-NAME"), row("B-NAME"), row("B-NAME"), row("B-NAME"),
        )

        val spans = decodeBioSpans(text, enc, logits, tags)

        assertEquals(listOf(LabeledSpan("NAME", 7..13)), spans)
    }

    @Test
    fun `I tag continues the previous span across words`() {
        val text = "jan kowalski"
        val enc = TokenEncoding(longArrayOf(0, 1, 2, 3), listOf(null, 0..2, 4..11, null))
        val logits = arrayOf(row("O"), row("B-NAME"), row("I-NAME"), row("O"))

        assertEquals(listOf(LabeledSpan("NAME", 0..11)), decodeBioSpans(text, enc, logits, tags))
    }

    @Test
    fun `an O word ends the span`() {
        val text = "jan do kowalski"
        val enc = TokenEncoding(longArrayOf(0, 1, 2, 3, 4), listOf(null, 0..2, 4..5, 7..14, null))
        val logits = arrayOf(row("O"), row("B-NAME"), row("O"), row("B-NAME"), row("O"))

        assertEquals(
            listOf(LabeledSpan("NAME", 0..2), LabeledSpan("NAME", 7..14)),
            decodeBioSpans(text, enc, logits, tags),
        )
    }
}
