package com.zpi.slowomir.overlay.log.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ActionDefEntity::class,
        ActionEventEntity::class,
        PredictionObservationEntity::class,
        PredictionSuppressionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SlowomirDatabase : RoomDatabase() {
    abstract fun actionDao(): ActionDao
    abstract fun predictionDao(): PredictionDao

    companion object {
        const val NAME = "slowomir.db"
    }
}
