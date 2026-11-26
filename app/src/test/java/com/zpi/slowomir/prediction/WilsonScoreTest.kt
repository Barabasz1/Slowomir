package com.zpi.slowomir.prediction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WilsonScoreTest {

    private val engine = VariableOrderMarkovEngine(org.mockito.kotlin.mock())

    @Test
    fun `boundary cases for z = 1_0 across orders`() {
        // 1 hit, 1 unique target -> PPM-C n = 1 + 1 = 2 -> ~0.211 (< order 2 threshold 0.38)
        val score1of2 = engine.wilsonLower(1.0, 2.0, z = 1.0)
        assertEquals(0.211, score1of2, 0.01)
        assertTrue(score1of2 < PredictionConfig.MIN_WILSON_ORDER_2)

        // 2 hits, 1 unique target -> PPM-C n = 2 + 1 = 3 -> ~0.386 (>= order 2 threshold 0.38)
        val score2of3 = engine.wilsonLower(2.0, 3.0, z = 1.0)
        assertEquals(0.386, score2of3, 0.01)
        assertTrue(score2of3 >= PredictionConfig.MIN_WILSON_ORDER_2)

        // 3 hits, 1 unique target -> PPM-C n = 3 + 1 = 4 -> 0.500 (>= order 1 threshold 0.44)
        val score3of4 = engine.wilsonLower(3.0, 4.0, z = 1.0)
        assertEquals(0.500, score3of4, 0.005)
        assertTrue(score3of4 >= PredictionConfig.MIN_WILSON_ORDER_1)

        // 4 hits, 1 unique target -> PPM-C n = 4 + 1 = 5 -> ~0.579 (>= order 0 threshold 0.50)
        val score4of5 = engine.wilsonLower(4.0, 5.0, z = 1.0)
        assertEquals(0.579, score4of5, 0.01)
        assertTrue(score4of5 >= PredictionConfig.MIN_WILSON_ORDER_0)

        // 5 hits, 1 alternative (6 total, 2 unique -> n = 8) -> ~0.449 (>= order 1 0.44)
        val score5of8 = engine.wilsonLower(5.0, 8.0, z = 1.0)
        assertEquals(0.449, score5of8, 0.01)
        assertTrue(score5of8 >= PredictionConfig.MIN_WILSON_ORDER_1)
    }

    @Test
    fun `ppm diversity or dismisses reduce score below order thresholds`() {
        // 2 hits on target, 1 on other, 2 unique targets -> n_eff = 3 + 2 = 5 -> ~0.216
        val scattered = engine.wilsonLower(2.0, 5.0, z = 1.0)
        assertTrue("Expected < order 2 threshold, was $scattered", scattered < PredictionConfig.MIN_WILSON_ORDER_2)

        // 3 hits after 1 dismiss (weight 1.5) -> n_eff = 3 + 1 + 1.5 = 5.5 -> ~0.343
        val dismissed = engine.wilsonLower(3.0, 5.5, z = 1.0)
        assertTrue("Expected < 0.38 after dismiss, was $dismissed", dismissed < PredictionConfig.MIN_WILSON_ORDER_2)
    }

    @Test
    fun `zero or negative k or n returns zero`() {
        assertEquals(0.0, engine.wilsonLower(0.0, 0.0), 0.0001)
        assertEquals(0.0, engine.wilsonLower(0.0, 5.0), 0.0001)
        assertEquals(0.0, engine.wilsonLower(2.0, 0.0), 0.0001)
    }
}
