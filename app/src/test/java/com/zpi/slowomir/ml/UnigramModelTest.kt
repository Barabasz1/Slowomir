package com.zpi.slowomir.ml

import org.junit.Assert.assertEquals
import org.junit.Test

class UnigramModelTest {

    // ids: <s>=0 <pad>=1 </s>=2 <unk>=3
    private val pieces = listOf("<s>", "<pad>", "</s>", "<unk>", "ot", "wórz", "o", "t", "w", "ó", "rz", "you", "tube", "youtube")
    private val scores = floatArrayOf(0f, 0f, 0f, 0f, -2f, -3f, -4f, -4f, -4f, -4f, -4f, -3f, -3f, -5f)

    private val model = UnigramModel(pieces, scores, unkId = 3, bosId = 0, eosId = 2)

    @Test
    fun `wraps words in special tokens and prefers the best scoring segmentation`() {
        val enc = model.encode("otwórz youtube", maxLength = 64)

        // "youtube" = you+tube (-6) beats youtube (-5)? no: -5 > -6, so the single piece wins.
        assertEquals(listOf(0L, 4L, 5L, 13L, 2L), enc.ids.toList())
    }

    @Test
    fun `offsets point back into the original text and special tokens are null`() {
        val enc = model.encode("otwórz  youtube", maxLength = 64)

        assertEquals(listOf(null, 0..1, 2..5, 8..14, null), enc.offsets)
    }

    @Test
    fun `unknown characters become a single fused unk`() {
        val enc = model.encode("zzz", maxLength = 64)

        assertEquals(listOf(0L, 3L, 2L), enc.ids.toList())
        assertEquals(listOf(null, 0..2, null), enc.offsets)
    }

    @Test
    fun `truncation keeps room for the closing token`() {
        val enc = model.encode("otwórz youtube", maxLength = 4)

        assertEquals(listOf(0L, 4L, 5L, 2L), enc.ids.toList())
    }

    @Test
    fun `empty text yields only special tokens`() {
        assertEquals(listOf(0L, 2L), model.encode("   ", maxLength = 64).ids.toList())
    }
}
