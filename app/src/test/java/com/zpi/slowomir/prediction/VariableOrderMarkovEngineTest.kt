package com.zpi.slowomir.prediction

import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.log.db.PredictionObservationEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class VariableOrderMarkovEngineTest {

    private val dao = mock<PredictionDao>()
    private val engine = VariableOrderMarkovEngine(dao)
    private val now = 1_000_000_000L

    private fun stat(
        key: String,
        order: Int,
        targetId: Long,
        hits: Double,
        dismisses: Double = 0.0,
        updatedAt: Long = now,
        cooldownUntil: Long = 0L,
    ) = PredictionObservationEntity(
        contextHash = key,
        targetActionDefId = targetId,
        contextOrder = order,
        packageName = "com.android.settings",
        decayedHits = hits,
        decayedDismisses = dismisses,
        lastUpdatedAt = updatedAt,
        cooldownUntilTimestamp = cooldownUntil,
    )

    @Test
    fun `order 2 deterministic 2 hits succeeds while scattered 2 vs 1 fails due to PPM-C`() = runBlocking {
        whenever(dao.getUiClickStats("k0")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k1")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k2")).thenReturn(listOf(stat("k2", 2, 100L, 2.0)))

        val ok = engine.predict("k0", "k1", "k2", now)
        assertNotNull(ok)
        assertEquals(100L, ok?.targetId)
        assertEquals(2, ok?.order)

        // 2 hits on 100L + 1 hit on 200L -> |Sigma_s| = 2, nEff = 3 + 2 = 5 -> Wilson(2,5) = 0.216 < 0.38
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(stat("k2", 2, 100L, 2.0), stat("k2", 2, 200L, 1.0))
        )
        assertNull(engine.predict("k0", "k1", "k2", now))
    }

    @Test
    fun `order 1 fallback succeeds when order 2 lacks support and is blocked by order 2 conflict`() = runBlocking {
        whenever(dao.getUiClickStats("k0")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k1")).thenReturn(listOf(stat("k1", 1, 100L, 3.0)))
        whenever(dao.getUiClickStats("k2")).thenReturn(listOf(stat("k2", 2, 100L, 1.0)))

        val fallback = engine.predict("k0", "k1", "k2", now)
        assertNotNull(fallback)
        assertEquals(100L, fallback?.targetId)
        assertEquals(1, fallback?.order)

        // Conflict in order 2: >= 1.5 hits pointing to 200L blocks order 1 predicting 100L
        whenever(dao.getUiClickStats("k2")).thenReturn(listOf(stat("k2", 2, 200L, 1.6)))
        assertNull(engine.predict("k0", "k1", "k2", now))
    }

    @Test
    fun `order 0 fallback succeeds for dominant screen target and rejects scattered screen`() = runBlocking {
        whenever(dao.getUiClickStats("k1")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k0")).thenReturn(listOf(stat("k0", 0, 100L, 4.0)))

        val ok = engine.predict("k0", "k1", null, now)
        assertNotNull(ok)
        assertEquals(100L, ok?.targetId)
        assertEquals(0, ok?.order)

        // Scattered screen: 4 hits on 100L + 3 alternatives (1 hit each) -> |Sigma_s|=4, nEff = 7 + 4 = 11 -> Wilson(4,11)=0.23 < 0.50
        whenever(dao.getUiClickStats("k0")).thenReturn(
            listOf(
                stat("k0", 0, 100L, 4.0),
                stat("k0", 0, 200L, 1.0),
                stat("k0", 0, 300L, 1.0),
                stat("k0", 0, 400L, 1.0),
            )
        )
        assertNull(engine.predict("k0", "k1", null, now))

        // Order 1 with 5/6 dominance (5 hits on 100L + 1 hit on 200L) succeeds with 0.44 threshold
        whenever(dao.getUiClickStats("k1")).thenReturn(
            listOf(stat("k1", 1, 100L, 5.0), stat("k1", 1, 200L, 1.0))
        )
        val dominantOrder1 = engine.predict("k0", "k1", null, now)
        assertNotNull(dominantOrder1)
        assertEquals(100L, dominantOrder1?.targetId)

        // Conflict in order 1 blocks order 0
        whenever(dao.getUiClickStats("k0")).thenReturn(listOf(stat("k0", 0, 100L, 5.0)))
        whenever(dao.getUiClickStats("k1")).thenReturn(listOf(stat("k1", 1, 200L, 1.6)))
        assertNull(engine.predict("k0", "k1", null, now))
    }

    @Test
    fun `time decay lets fresh habit beat stale observations`() = runBlocking {
        val threeHalfLivesAgo = now - 3L * PredictionConfig.HALF_LIFE_MS // factor = 0.125
        // Stale target had 16 hits -> decayed to 2.0
        // Fresh target has 6 hits now -> |Sigma_s|=2, nEff = 8 + 2 = 10 -> Wilson(6, 10, 1.0) ~= 0.445 >= 0.44
        whenever(dao.getUiClickStats("k0")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k1")).thenReturn(
            listOf(
                stat("k1", 1, 100L, hits = 16.0, updatedAt = threeHalfLivesAgo),
                stat("k1", 1, 200L, hits = 6.0, updatedAt = now),
            )
        )

        val candidate = engine.predict("k0", "k1", null, now)
        assertNotNull(candidate)
        assertEquals(200L, candidate?.targetId)
        assertTrue((candidate?.effectiveHits ?: 0.0) >= PredictionConfig.MIN_SUPPORT_ORDER_1)
    }

    @Test
    fun `dismiss weight lowers wilson below threshold and cooldown skips target`() = runBlocking {
        whenever(dao.getUiClickStats("k0")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k1")).thenReturn(emptyList())
        // 2 hits with 1.5 dismiss weight -> nEff = 2 + 1 + 1.5 = 4.5 -> Wilson(2, 4.5) = 0.250 < 0.38
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(stat("k2", 2, 100L, hits = 2.0, dismisses = 1.5, cooldownUntil = 0L))
        )
        assertNull(engine.predict("k0", "k1", "k2", now))

        // Strong hits but active cooldown -> skipped
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(stat("k2", 2, 100L, hits = 10.0, dismisses = 0.0, cooldownUntil = now + 60_000L))
        )
        assertNull(engine.predict("k0", "k1", "k2", now))
    }

    @Test
    fun `active cooldown on order 2 propagates downward to block order 1 and order 0`() = runBlocking {
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(stat("k2", 2, 100L, hits = 10.0, cooldownUntil = now + 60_000L))
        )
        whenever(dao.getUiClickStats("k1")).thenReturn(
            listOf(stat("k1", 1, 100L, hits = 10.0, cooldownUntil = 0L))
        )
        whenever(dao.getUiClickStats("k0")).thenReturn(
            listOf(stat("k0", 0, 100L, hits = 10.0, cooldownUntil = 0L))
        )

        assertNull(engine.predict("k0", "k1", "k2", now))
    }

    @Test
    fun `continuous soft sigma scales fading targets smoothly and order 2 skips lower SQL queries`() = runBlocking<Unit> {
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(
                stat("k2", 2, 100L, hits = 2.5),
                stat("k2", 2, 200L, hits = 0.1),
            )
        )

        val candidate = engine.predict("k0", "k1", "k2", now)
        assertNotNull(candidate)
        assertEquals(100L, candidate?.targetId)
        assertEquals(3.7, candidate?.effectiveTotal ?: 0.0, 0.001)
        verify(dao, never()).getUiClickStats("k1")
        verify(dao, never()).getUiClickStats("k0")
    }

    @Test
    fun `higher order favoring same target does not conflict while stronger alternative blocks`() = runBlocking<Unit> {
        // Order 1 strongly predicts 100L
        whenever(dao.getUiClickStats("k0")).thenReturn(emptyList())
        whenever(dao.getUiClickStats("k1")).thenReturn(listOf(stat("k1", 1, 100L, hits = 10.0)))

        // Order 2 has sameHits=2.0 for 100L and otherHits=1.5 for 200L (does not exceed sameHits -> NOT a conflict)
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(stat("k2", 2, 100L, hits = 2.0), stat("k2", 2, 200L, hits = 1.5))
        )
        val allowed = engine.predict("k0", "k1", "k2", now)
        assertNotNull(allowed)
        assertEquals(100L, allowed?.targetId)
        assertEquals(1, allowed?.order)

        // Order 2 has otherHits=1.6 > sameHits=1.0 -> real conflict -> blocks order 1 fallback
        whenever(dao.getUiClickStats("k2")).thenReturn(
            listOf(stat("k2", 2, 100L, hits = 1.0), stat("k2", 2, 200L, hits = 1.6))
        )
        assertNull(engine.predict("k0", "k1", "k2", now))
    }
}
