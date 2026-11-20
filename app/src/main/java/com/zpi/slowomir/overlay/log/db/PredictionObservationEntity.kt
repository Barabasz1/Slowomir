package com.zpi.slowomir.overlay.log.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prediction_observation",
    foreignKeys = [
        ForeignKey(
            entity = ActionDefEntity::class,
            parentColumns = ["id_action_def"],
            childColumns = ["target_action_def_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["context_hash", "id_observation"]),
        Index(value = ["target_action_def_id"]),
        Index(value = ["package_name"]),
    ],
)
data class PredictionObservationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_observation")
    val idObservation: Long = 0,

    @ColumnInfo(name = "context_hash")
    val contextHash: String,

    @ColumnInfo(name = "context_order")
    val contextOrder: Int, // 1 or 2

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "target_action_def_id")
    val targetActionDefId: Long,
)
