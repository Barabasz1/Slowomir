package com.zpi.slowomir.prediction

import com.zpi.slowomir.overlay.log.db.PredictionDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class Candidate(val targetId: Long, val key: String, val k: Int, val n: Int)

@Singleton
class VariableOrderMarkovEngine @Inject constructor(
    private val dao: PredictionDao,
) {
    suspend fun predict(key1: String, key2: String?): Candidate? {
        val w2 = key2?.let { dao.lastTargets(it, PredictionConfig.WINDOW_M) }
        if (key2 != null && w2 != null && w2.size >= PredictionConfig.MIN_SUPPORT) {
            return decide(key2, w2)
        }

        val w1 = dao.lastTargets(key1, PredictionConfig.WINDOW_M)
        if (w1.size < PredictionConfig.MIN_SUPPORT) return null
        val c = decide(key1, w1) ?: return null

        if (w2 != null && w2.count { it != c.targetId } >= PredictionConfig.CONFLICT_MIN) return null
        return c
    }

    fun decide(key: String, window: List<Long>): Candidate? {
        val n = window.size
        if (n == 0) return null
        val (target, k) = window.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return null
        val p = k.toDouble() / n
        return if (p >= PredictionConfig.MIN_CONFIDENCE && wilsonLower(k, n) >= PredictionConfig.MIN_WILSON) {
            Candidate(target, key, k, n)
        } else {
            null
        }
    }

    fun wilsonLower(k: Int, n: Int, z: Double = PredictionConfig.WILSON_Z): Double {
        if (n == 0) return 0.0
        val p = k.toDouble() / n
        val z2 = z * z
        val centre = p + z2 / (2.0 * n)
        val margin = z * sqrt(p * (1.0 - p) / n + z2 / (4.0 * n * n))
        return (centre - margin) / (1.0 + z2 / n)
    }
}
