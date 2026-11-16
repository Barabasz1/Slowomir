package com.zpi.slowomir.overlay.log.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class ActionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertDef(def: ActionDefEntity): Long

    @Query("SELECT id_action_def FROM action_def WHERE action_hash = :hash")
    abstract suspend fun findDefId(hash: String): Long?

    @Insert
    abstract suspend fun insertEvent(event: ActionEventEntity): Long

    @Transaction
    open suspend fun logEvent(def: ActionDefEntity, timestamp: Long): LoggedAction {
        val insertedId = insertDef(def)
        val defInserted = insertedId != -1L
        val defId = if (defInserted) insertedId else checkNotNull(findDefId(def.actionHash)) {
            "Action definition ${def.actionHash} should exist"
        }
        val event = ActionEventEntity(idActionDef = defId, timestamp = timestamp)
        val eventId = insertEvent(event)
        return LoggedAction(
            def = def.copy(idActionDef = defId),
            defInserted = defInserted,
            event = event.copy(idActionEvent = eventId),
        )
    }


    @Query("DELETE FROM action_event WHERE timestamp < :cutoff")
    abstract suspend fun deleteEventsOlderThan(cutoff: Long): Int

    @Query(
        "DELETE FROM action_def WHERE id_action_def NOT IN " +
                "(SELECT DISTINCT id_action_def FROM action_event)"
    )
    abstract suspend fun deleteUnusedDefs(): Int

    @Transaction
    open suspend fun prune(cutoff: Long) {
        deleteEventsOlderThan(cutoff)
        deleteUnusedDefs()
    }

    @Query("DELETE FROM action_event")
    abstract suspend fun deleteAllEvents(): Int

    @Query("DELETE FROM action_def")
    abstract suspend fun deleteAllDefs(): Int

    @Transaction
    open suspend fun clearAll() {
        deleteAllEvents()
        deleteAllDefs()
    }
}

data class LoggedAction(
    val def: ActionDefEntity,
    val defInserted: Boolean,
    val event: ActionEventEntity,
)