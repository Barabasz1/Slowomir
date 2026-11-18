package com.zpi.slowomir.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ActionDefEntity::class, ActionEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SlowomirDatabase : RoomDatabase() {
    abstract fun actionDao(): ActionDao

    companion object {
        const val NAME = "slowomir.db"
    }
}