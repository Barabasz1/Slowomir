package com.zpi.slowomir.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zpi.slowomir.overlay.log.db.ActionDao
import com.zpi.slowomir.overlay.log.db.ActionDefEntity
import com.zpi.slowomir.overlay.log.db.ActionEventEntity
import com.zpi.slowomir.overlay.log.db.PredictionDao
import com.zpi.slowomir.overlay.log.db.PredictionObservationEntity

@Database(
    entities = [
        ActionDefEntity::class,
        ActionEventEntity::class,
        PredictionObservationEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class SlowomirDatabase : RoomDatabase() {
    abstract fun actionDao(): ActionDao
    abstract fun predictionDao(): PredictionDao

    companion object {
        const val NAME = "slowomir.db"
    }
}