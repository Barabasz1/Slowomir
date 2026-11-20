package com.zpi.slowomir.prediction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WilsonScoreTest {

    private val engine = VariableOrderMarkovEngine(org.mockito.kotlin.mock())

    @Test
    fun `test small samples and boundary cases below threshold`() {
        val score4of5 = engine.wilsonLower(4, 5)
        assertTrue("4 of 5 should be < 0.60, was $score4of5", score4of5 < 0.60)
        assertEquals(0.38, score4of5, 0.05)

        val score9of10 = engine.wilsonLower(9, 10)
        assertTrue("9 of 10 should be < 0.60 (~0.596), was $score9of10", score9of10 < 0.60)
        assertEquals(0.596, score9of10, 0.01)
    }

    @Test
    fun `test accepted cases above threshold`() {
        val score10of10 = engine.wilsonLower(10, 10)
        assertTrue("10 of 10 should be >= 0.60 (~0.72), was $score10of10", score10of10 >= 0.60)
        assertEquals(0.72, score10of10, 0.02)

        val score19of20 = engine.wilsonLower(19, 20)
        assertTrue("19 of 20 should be >= 0.60 (~0.76), was $score19of20", score19of20 >= 0.60)
        assertEquals(0.76, score19of20, 0.02)
    }

    @Test
    fun `test zero n returns zero`() {
        assertEquals(0.0, engine.wilsonLower(0, 0), 0.001)
    }
}
