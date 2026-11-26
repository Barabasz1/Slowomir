package com.zpi.slowomir.overlay.log.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "prediction_observation",
    primaryKeys = ["context_hash", "target_action_def_id"],
    foreignKeys = [
        ForeignKey(
            entity = ActionDefEntity::class,
            parentColumns = ["id_action_def"],
            childColumns = ["target_action_def_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["context_hash"]),
        Index(value = ["target_action_def_id"]),
        Index(value = ["package_name"]),
        Index(value = ["last_updated_at"]),
    ],
)
data class PredictionObservationEntity(
    @ColumnInfo(name = "context_hash")
    val contextHash: String,

    @ColumnInfo(name = "target_action_def_id")
    val targetActionDefId: Long,

    @ColumnInfo(name = "context_order")
    val contextOrder: Int,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "decayed_hits")
    val decayedHits: Double = 0.0,

    @ColumnInfo(name = "decayed_dismisses")
    val decayedDismisses: Double = 0.0,

    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = 0L,

    @ColumnInfo(name = "consecutive_dismiss_count")
    val consecutiveDismissCount: Int = 0,

    @ColumnInfo(name = "cooldown_until_timestamp")
    val cooldownUntilTimestamp: Long = 0L,
)
