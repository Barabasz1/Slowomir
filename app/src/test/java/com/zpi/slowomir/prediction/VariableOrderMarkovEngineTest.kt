package com.zpi.slowomir.prediction

import com.zpi.slowomir.overlay.log.db.PredictionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class VariableOrderMarkovEngineTest {

    private val dao = mock<PredictionDao>()
    private val engine = VariableOrderMarkovEngine(dao)

    @Test
    fun `order 2 with sufficient data and confidence returns candidate`() = runBlocking {
        val key2 = "key2"
        val key1 = "key1"
        // 10 times target 100L out of 10
        val window = List(10) { 100L }
        whenever(dao.lastTargets(key2, PredictionConfig.WINDOW_M)).thenReturn(window)

        val candidate = engine.predict(key1, key2)
        assertNotNull(candidate)
        assertEquals(100L, candidate?.targetId)
        assertEquals(key2, candidate?.key)
    }

    @Test
    fun `order 2 with sufficient data but ambiguous returns null without backoff`() = runBlocking {
        val key2 = "key2"
        val key1 = "key1"
        // 5 times 100L, 5 times 200L -> ambiguous (50%)
        val window = List(5) { 100L } + List(5) { 200L }
        whenever(dao.lastTargets(key2, PredictionConfig.WINDOW_M)).thenReturn(window)
        whenever(dao.lastTargets(key1, PredictionConfig.WINDOW_M)).thenReturn(List(10) { 100L })

        val candidate = engine.predict(key1, key2)
        assertNull("Ambiguous order 2 must return null and not fallback to order 1", candidate)
    }

    @Test
    fun `order 2 with insufficient data falls back to order 1`() = runBlocking {
        val key2 = "key2"
        val key1 = "key1"
        // Order 2 has only 1 sample (< 8)
        whenever(dao.lastTargets(key2, PredictionConfig.WINDOW_M)).thenReturn(listOf(100L))
        // Order 1 has 10 samples of 100L
        whenever(dao.lastTargets(key1, PredictionConfig.WINDOW_M)).thenReturn(List(10) { 100L })

        val candidate = engine.predict(key1, key2)
        assertNotNull(candidate)
        assertEquals(100L, candidate?.targetId)
        assertEquals(key1, candidate?.key)
    }

    @Test
    fun `order 2 with conflicting observations blocks order 1 fallback`() = runBlocking {
        val key2 = "key2"
        val key1 = "key1"
        // Order 2 has 3 samples, all 200L (contradicting Order 1's 100L)
        whenever(dao.lastTargets(key2, PredictionConfig.WINDOW_M)).thenReturn(listOf(200L, 200L, 200L))
        // Order 1 strongly prefers 100L
        whenever(dao.lastTargets(key1, PredictionConfig.WINDOW_M)).thenReturn(List(10) { 100L })

        val candidate = engine.predict(key1, key2)
        assertNull("Contradiction from order 2 should suppress prediction", candidate)
    }
}
