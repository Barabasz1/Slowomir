package com.zpi.slowomir.overlay.log.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class PredictionDao {

    @Insert
    abstract suspend fun insertObservation(o: PredictionObservationEntity): Long

    @Query("SELECT target_action_def_id FROM prediction_observation WHERE context_hash = :hash ORDER BY id_observation DESC LIMIT :m")
    abstract suspend fun lastTargets(hash: String, m: Int): List<Long>

    @Query(
        "SELECT o.target_action_def_id FROM prediction_observation o " +
                "INNER JOIN action_def d ON o.target_action_def_id = d.id_action_def " +
                "WHERE o.context_hash = :hash AND d.action_type = 'UI_CLICK' " +
                "ORDER BY o.id_observation DESC LIMIT 1"
    )
    abstract suspend fun lastUiClickTarget(hash: String): Long?

    @Query(
        "DELETE FROM prediction_observation WHERE context_hash = :hash AND id_observation NOT IN " +
                "(SELECT id_observation FROM prediction_observation WHERE context_hash = :hash ORDER BY id_observation DESC LIMIT :m)"
    )
    abstract suspend fun pruneWindow(hash: String, m: Int)

    @Query(
        "DELETE FROM prediction_observation WHERE id_observation NOT IN " +
                "(SELECT id_observation FROM prediction_observation ORDER BY id_observation DESC LIMIT :max)"
    )
    abstract suspend fun pruneGlobal(max: Int)

    @Transaction
    open suspend fun record(o: PredictionObservationEntity, m: Int) {
        insertObservation(o)
        pruneWindow(o.contextHash, m)
    }

    @Query("SELECT * FROM prediction_suppression WHERE context_hash = :hash AND target_action_def_id = :target")
    abstract suspend fun getSuppression(hash: String, target: Long): PredictionSuppressionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSuppression(s: PredictionSuppressionEntity)

    @Query("DELETE FROM prediction_suppression WHERE cooldown_until_timestamp > 0 AND cooldown_until_timestamp < :now")
    abstract suspend fun pruneExpiredSuppressions(now: Long)

    @Query("DELETE FROM prediction_observation WHERE package_name = :pkg")
    abstract suspend fun deleteForPackage(pkg: String)

    @Query("SELECT * FROM action_def WHERE id_action_def = :id LIMIT 1")
    abstract suspend fun getActionDef(id: Long): ActionDefEntity?

    @Query("SELECT * FROM action_def WHERE action_hash = :hash LIMIT 1")
    abstract suspend fun getActionDefByHash(hash: String): ActionDefEntity?
}
