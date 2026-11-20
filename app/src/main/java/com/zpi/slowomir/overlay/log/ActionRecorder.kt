package com.zpi.slowomir.overlay.log

import android.util.Log
import com.zpi.slowomir.overlay.log.db.ActionDao
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.LoggedAction
import com.zpi.slowomir.prediction.PredictionManager
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionRecorder @Inject constructor(
    private val actionDao: ActionDao,
    private val predictionManagerProvider: Provider<PredictionManager>,
) {
    private val worker = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + worker)

    private var lastPruneAt = 0L

    init {
        if (ActionLogConfig.isEnabled) {
            scope.launch { prune() }
        }
    }

    fun record(action: ActionDescriptor, timestamp: Long = System.currentTimeMillis()) {
        if (!ActionLogConfig.isEnabled) return

        scope.launch {
            try {
                val stored = actionDao.logEvent(action.toEntity(), timestamp)
                if (ActionLogConfig.logRowsToConsole) printRows(stored)
                try {
                    predictionManagerProvider.get().onActionRecorded(action, stored.def.actionHash, stored.def.idActionDef)
                } catch (e: Exception) {
                    Log.e(TAG, "Prediction observation failed", e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save action ${action.type}", e)
                return@launch
            }

            if (isPruneDue()) prune()
        }
    }

    suspend fun clearHistory() {
        withContext(worker) {
            actionDao.clearAll()
        }
    }

    private fun printRows(stored: LoggedAction) {
        val def = stored.def
        val event = stored.event

        if (stored.defInserted) {
            Log.d(
                ActionLogConfig.CONSOLE_TAG,
                "action_def   INSERT: " +
                        "id_action_def=${def.idActionDef}, " +
                        "action_hash=${def.actionHash}, " +
                        "action_type=${def.actionType}, " +
                        "package_name=${def.packageName}, " +
                        "resource_id=${def.resourceId}, " +
                        "hierarchy_path=${def.hierarchyPath}, " +
                        "label_hash=${def.labelHash}, " +
                        "command_type=${def.commandType}, " +
                        "command_args=${def.commandArgs}"
            )
        }

        val defNote = if (stored.defInserted) "new definition" else "existing definition"
        Log.d(
            ActionLogConfig.CONSOLE_TAG,
            "action_event INSERT: " +
                    "id_action_event=${event.idActionEvent}, " +
                    "id_action_def=${event.idActionDef} ($defNote), " +
                    "timestamp=${event.timestamp} (${formatTime(event.timestamp)})"
        )
    }

    private fun formatTime(epochMillis: Long): String =
        TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    private fun isPruneDue(): Boolean {
        val now = System.currentTimeMillis()
        return now < lastPruneAt ||
                now - lastPruneAt >= TimeUnit.HOURS.toMillis(ActionLogConfig.PRUNE_INTERVAL_HOURS)
    }

    private suspend fun prune() {
        val now = System.currentTimeMillis()
        lastPruneAt = now
        try {
            val cutoff = now - TimeUnit.DAYS.toMillis(ActionLogConfig.RETENTION_DAYS)
            actionDao.prune(cutoff)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune action history", e)
        }
    }

    private fun ActionDescriptor.toEntity(): ActionDefEntity {
        val isClick = type == ActionType.UI_CLICK
        return ActionDefEntity(
            actionHash = ActionHasher.hash(this),
            actionType = type,
            packageName = packageName,
            resourceId = if (isClick) resourceId else null,
            hierarchyPath = if (isClick) hierarchyPath else null,
            labelHash = if (isClick) labelHash else null,
            commandType = commandType,
            commandArgs = commandArgs,
        )
    }

    private companion object {
        const val TAG = "ActionRecorder"
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
