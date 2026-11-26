package com.zpi.slowomir.overlay.log.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlin.math.min
import kotlin.math.pow

@Dao
abstract class PredictionDao {

    @Query(
        "SELECT o.* FROM prediction_observation o " +
            "INNER JOIN action_def d ON o.target_action_def_id = d.id_action_def " +
            "WHERE o.context_hash = :hash AND d.action_type = 'UI_CLICK'"
    )
    abstract suspend fun getUiClickStats(hash: String): List<PredictionObservationEntity>

    @Query(
        "SELECT * FROM prediction_observation " +
            "WHERE context_hash = :hash AND target_action_def_id = :target LIMIT 1"
    )
    abstract suspend fun getStat(hash: String, target: Long): PredictionObservationEntity?

    @Upsert
    abstract suspend fun upsert(stat: PredictionObservationEntity)

    @Transaction
    open suspend fun recordHit(
        contextHash: String,
        contextOrder: Int,
        packageName: String,
        targetActionDefId: Long,
        now: Long,
        halfLifeMs: Long,
        maxDecayedHits: Double = 15.0,
        hitDismissDecayFactor: Double = 0.7,
    ) {
        val existing = getStat(contextHash, targetActionDefId)
        val factor = if (existing != null && now > existing.lastUpdatedAt && halfLifeMs > 0L) {
            2.0.pow(-(now - existing.lastUpdatedAt).toDouble() / halfLifeMs.toDouble())
        } else {
            1.0
        }
        val updatedHits = min((existing?.decayedHits ?: 0.0) * factor + 1.0, maxDecayedHits)
        val updatedDismisses = (existing?.decayedDismisses ?: 0.0) * factor * hitDismissDecayFactor

        upsert(
            PredictionObservationEntity(
                contextHash = contextHash,
                targetActionDefId = targetActionDefId,
                contextOrder = contextOrder,
                packageName = packageName,
                decayedHits = updatedHits,
                decayedDismisses = updatedDismisses,
                lastUpdatedAt = now,
                consecutiveDismissCount = 0,
                cooldownUntilTimestamp = 0L,
            )
        )
    }

    @Transaction
    open suspend fun recordImplicitDismiss(
        contextHash: String,
        targetActionDefId: Long,
        now: Long,
        halfLifeMs: Long,
        weight: Double,
        maxDecayedDismisses: Double = 6.0,
    ) {
        val existing = getStat(contextHash, targetActionDefId) ?: return
        val factor = if (now > existing.lastUpdatedAt && halfLifeMs > 0L) {
            2.0.pow(-(now - existing.lastUpdatedAt).toDouble() / halfLifeMs.toDouble())
        } else {
            1.0
        }
        upsert(
            existing.copy(
                decayedHits = existing.decayedHits * factor,
                decayedDismisses = min(existing.decayedDismisses * factor + weight, maxDecayedDismisses),
                lastUpdatedAt = now,
            )
        )
    }

    @Transaction
    open suspend fun recordDismiss(
        contextHash: String,
        targetActionDefId: Long,
        now: Long,
        halfLifeMs: Long,
        dismissWeight: Double,
        cooldownDurationsMs: LongArray,
        maxDecayedDismisses: Double = 6.0,
    ): Int {
        val existing = getStat(contextHash, targetActionDefId) ?: return 0
        val factor = if (now > existing.lastUpdatedAt && halfLifeMs > 0L) {
            2.0.pow(-(now - existing.lastUpdatedAt).toDouble() / halfLifeMs.toDouble())
        } else {
            1.0
        }
        val count = existing.consecutiveDismissCount + 1
        val cooldownIndex = min(count - 1, cooldownDurationsMs.size - 1)
        val cooldownDuration = cooldownDurationsMs[cooldownIndex]

        upsert(
            existing.copy(
                decayedHits = existing.decayedHits * factor,
                decayedDismisses = min(existing.decayedDismisses * factor + dismissWeight, maxDecayedDismisses),
                lastUpdatedAt = now,
                consecutiveDismissCount = count,
                cooldownUntilTimestamp = now + cooldownDuration,
            )
        )
        return count
    }

    @Transaction
    open suspend fun clearSuppression(contextHash: String, targetActionDefId: Long) {
        val existing = getStat(contextHash, targetActionDefId) ?: return
        upsert(
            existing.copy(
                consecutiveDismissCount = 0,
                cooldownUntilTimestamp = 0L,
            )
        )
    }

    @Query("DELETE FROM prediction_observation WHERE last_updated_at < :cutoffTimestamp")
    abstract suspend fun pruneStale(cutoffTimestamp: Long)

    @Query(
        "DELETE FROM prediction_observation WHERE rowid NOT IN " +
            "(SELECT rowid FROM prediction_observation ORDER BY last_updated_at DESC LIMIT :max)"
    )
    abstract suspend fun pruneGlobal(max: Int)

    @Query("DELETE FROM prediction_observation WHERE package_name = :pkg")
    abstract suspend fun deleteForPackage(pkg: String)

    @Query("SELECT * FROM action_def WHERE id_action_def = :id LIMIT 1")
    abstract suspend fun getActionDef(id: Long): ActionDefEntity?

    @Query("SELECT * FROM action_def WHERE action_hash = :hash LIMIT 1")
    abstract suspend fun getActionDefByHash(hash: String): ActionDefEntity?
}
