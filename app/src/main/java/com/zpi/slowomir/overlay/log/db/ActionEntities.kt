package com.zpi.slowomir.overlay.log.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zpi.slowomir.overlay.log.ActionType

@Entity(
    tableName = "action_def",
    indices = [Index(value = ["action_hash"], unique = true)],
)
data class ActionDefEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_action_def")
    val idActionDef: Long = 0,

    @ColumnInfo(name = "action_hash")
    val actionHash: String,

    @ColumnInfo(name = "action_type")
    val actionType: ActionType,

    @ColumnInfo(name = "package_name")
    val packageName: String? = null,

    @ColumnInfo(name = "resource_id")
    val resourceId: String? = null,

    @ColumnInfo(name = "hierarchy_path")
    val hierarchyPath: String? = null,

    @ColumnInfo(name = "label_hash")
    val labelHash: String? = null,
)

@Entity(
    tableName = "action_event",
    foreignKeys = [
        ForeignKey(
            entity = ActionDefEntity::class,
            parentColumns = ["id_action_def"],
            childColumns = ["id_action_def"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["id_action_def"])],
)
data class ActionEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_action_event")
    val idActionEvent: Long = 0,

    @ColumnInfo(name = "id_action_def")
    val idActionDef: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
