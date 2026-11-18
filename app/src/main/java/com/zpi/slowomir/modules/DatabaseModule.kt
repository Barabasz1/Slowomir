package com.zpi.slowomir.modules

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.zpi.slowomir.overlay.log.ActionLogConfig
import com.zpi.slowomir.db.ActionDao
import com.zpi.slowomir.db.SlowomirDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SlowomirDatabase {
        if (ActionLogConfig.clearOnStart) {
            val deleted = context.deleteDatabase(SlowomirDatabase.NAME)
            Log.i(TAG, "clearOnStart: database '${SlowomirDatabase.NAME}' deleted = $deleted")
        }
        return Room.databaseBuilder(context, SlowomirDatabase::class.java, SlowomirDatabase.NAME).build()
    }

    @Provides
    fun provideActionDao(database: SlowomirDatabase): ActionDao = database.actionDao()
}