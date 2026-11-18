package com.zpi.slowomir.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zpi.slowomir.overlay.log.ActionType

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

    @Query(
        "SELECT e.id_action_event AS idActionEvent, e.timestamp AS timestamp, " +
                "d.id_action_def AS idActionDef, d.action_hash AS actionHash, " +
                "d.action_type AS actionType, d.package_name AS packageName, " +
                "d.resource_id AS resourceId, d.hierarchy_path AS hierarchyPath, " +
                "d.label_hash AS labelHash, d.command_type AS commandType, " +
                "d.command_args AS commandArgs " +
                "FROM action_event e INNER JOIN action_def d ON e.id_action_def = d.id_action_def " +
                "ORDER BY e.timestamp ASC, e.id_action_event ASC"
    )
    abstract suspend fun getAllLoggedEvents(): List<LoggedActionEvent>

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

data class LoggedActionEvent(
    val idActionEvent: Long,
    val timestamp: Long,
    val idActionDef: Long,
    val actionHash: String,
    val actionType: ActionType,
    val packageName: String?,
    val resourceId: String?,
    val hierarchyPath: String?,
    val labelHash: String?,
    val commandType: String?,
    val commandArgs: String?,
)