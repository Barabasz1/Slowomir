package com.zpi.slowomir.prediction

import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.log.db.PredictionObservationEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class Candidate(
    val targetId: Long,
    val key: String,
    val order: Int,
    val effectiveHits: Double,
    val effectiveTotal: Double,
    val wilsonScore: Double,
)

@Singleton
class VariableOrderMarkovEngine @Inject constructor(
    private val dao: PredictionDao,
) {
    private data class DecayedEntry(
        val row: PredictionObservationEntity,
        val hits: Double,
        val dismisses: Double,
    )

    private data class ScoredEntry(
        val entry: DecayedEntry,
        val nEff: Double,
        val wilsonScore: Double,
    )

    fun decayFactor(deltaMs: Long, halfLifeMs: Long = PredictionConfig.HALF_LIFE_MS): Double {
        if (deltaMs <= 0L || halfLifeMs <= 0L) return 1.0
        return 2.0.pow(-deltaMs.toDouble() / halfLifeMs.toDouble())
    }

    suspend fun predict(
        key0: String,
        key1: String,
        key2: String?,
        now: Long = System.currentTimeMillis(),
    ): Candidate? {
        val stats2 = key2?.let { dao.getUiClickStats(it) }
        val cooldown2 = activeCooldownTargets(stats2, now)
        if (key2 != null && stats2 != null) {
            decide(key2, order = 2, stats = stats2, now = now)?.let { return it }
        }

        val stats1 = dao.getUiClickStats(key1)
        decide(key1, order = 1, stats = stats1, now = now, excludedTargets = cooldown2)?.let { c1 ->
            return if (!hasConflict(stats2, c1.targetId, now)) c1 else null
        }

        val cooldown1 = cooldown2 + activeCooldownTargets(stats1, now)
        val stats0 = dao.getUiClickStats(key0)
        decide(key0, order = 0, stats = stats0, now = now, excludedTargets = cooldown1)?.let { c0 ->
            if (!hasConflict(stats2, c0.targetId, now) && !hasConflict(stats1, c0.targetId, now)) {
                return c0
            }
        }

        return null
    }

    fun decide(
        key: String,
        order: Int,
        stats: List<PredictionObservationEntity>,
        now: Long = System.currentTimeMillis(),
        excludedTargets: Set<Long> = emptySet(),
    ): Candidate? {
        val active = stats.mapNotNull { row ->
            val factor = decayFactor(max(0L, now - row.lastUpdatedAt))
            val hits = row.decayedHits * factor
            if (hits < PredictionConfig.ACTIVE_WEIGHT_EPSILON) {
                null
            } else {
                DecayedEntry(
                    row = row,
                    hits = hits,
                    dismisses = row.decayedDismisses * factor,
                )
            }
        }
        if (active.isEmpty()) return null

        val effectiveUniqueCount = active.sumOf { min(1.0, it.hits) }
        val totalHits = active.sumOf { it.hits }

        val eligible = active.filter {
            now >= it.row.cooldownUntilTimestamp && it.row.targetActionDefId !in excludedTargets
        }
        if (eligible.isEmpty()) return null

        val best = eligible.map { entry ->
            val nEff = totalHits + effectiveUniqueCount + entry.dismisses
            ScoredEntry(
                entry = entry,
                nEff = nEff,
                wilsonScore = wilsonLower(entry.hits, nEff, PredictionConfig.WILSON_Z),
            )
        }.maxWithOrNull(compareBy<ScoredEntry> { it.wilsonScore }.thenBy { it.entry.hits }) ?: return null

        return if (best.entry.hits >= minSupport(order) && best.wilsonScore >= minWilson(order)) {
            Candidate(
                targetId = best.entry.row.targetActionDefId,
                key = key,
                order = order,
                effectiveHits = best.entry.hits,
                effectiveTotal = best.nEff,
                wilsonScore = best.wilsonScore,
            )
        } else {
            null
        }
    }

    private fun activeCooldownTargets(
        stats: List<PredictionObservationEntity>?,
        now: Long,
    ): Set<Long> {
        if (stats.isNullOrEmpty()) return emptySet()
        return stats.mapNotNullTo(mutableSetOf()) { row ->
            row.targetActionDefId.takeIf { now < row.cooldownUntilTimestamp }
        }
    }

    private fun hasConflict(
        higherStats: List<PredictionObservationEntity>?,
        targetId: Long,
        now: Long,
    ): Boolean {
        if (higherStats.isNullOrEmpty()) return false
        var sameHits = 0.0
        var otherHits = 0.0
        for (row in higherStats) {
            val hits = row.decayedHits * decayFactor(max(0L, now - row.lastUpdatedAt))
            if (hits < PredictionConfig.ACTIVE_WEIGHT_EPSILON) continue
            if (row.targetActionDefId == targetId) {
                sameHits += hits
            } else {
                otherHits += hits
            }
        }
        return otherHits >= PredictionConfig.CONFLICT_WEIGHT && otherHits > sameHits
    }

    fun wilsonLower(
        k: Double,
        n: Double,
        z: Double = PredictionConfig.WILSON_Z,
    ): Double {
        if (n <= 0.0 || k <= 0.0) return 0.0
        val p = (k / n).coerceIn(0.0, 1.0)
        val z2 = z * z
        val centre = p + z2 / (2.0 * n)
        val margin = z * sqrt(p * (1.0 - p) / n + z2 / (4.0 * n * n))
        return (centre - margin) / (1.0 + z2 / n)
    }

    private fun minSupport(order: Int): Double = when (order) {
        2 -> PredictionConfig.MIN_SUPPORT_ORDER_2
        1 -> PredictionConfig.MIN_SUPPORT_ORDER_1
        else -> PredictionConfig.MIN_SUPPORT_ORDER_0
    }

    private fun minWilson(order: Int): Double = when (order) {
        2 -> PredictionConfig.MIN_WILSON_ORDER_2
        1 -> PredictionConfig.MIN_WILSON_ORDER_1
        else -> PredictionConfig.MIN_WILSON_ORDER_0
    }
}
