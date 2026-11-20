package com.zpi.slowomir.modules

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zpi.slowomir.overlay.log.db.ActionDao
import com.zpi.slowomir.db.SlowomirDatabase
import com.zpi.slowomir.overlay.log.ActionLogConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        return Room.databaseBuilder(context, SlowomirDatabase::class.java, SlowomirDatabase.NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDatabase(db)
                }
            })
            .build()
    }

    @Provides
    fun provideActionDao(database: SlowomirDatabase): ActionDao = database.actionDao()

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.execSQL(
                """
                INSERT INTO action_def (id_action_def, action_hash, action_type, package_name, command_type, command_args)
                VALUES (1, 'hash_seed_home', 'HOME', 'com.android.launcher3', 'com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand', '{}')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO action_def (id_action_def, action_hash, action_type, package_name, command_type, command_args)
                VALUES (2, 'hash_seed_open_settings', 'COMMAND', 'com.android.settings', 'com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand', '{"packageName":"com.android.settings"}')
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO action_def (id_action_def, action_hash, action_type, package_name, resource_id, hierarchy_path, command_type, command_args)
                VALUES (3, 'hash_seed_tap_settings', 'UI_CLICK', 'com.android.settings', 'com.android.settings:id/search_bar', 'FrameLayout[0]/LinearLayout[0]', 'com.zpi.slowomir.overlay.commands.touch.TapCommand', '{"x":300.0,"y":150.0}')
                """.trimIndent()
            )

            db.execSQL("INSERT INTO action_event (id_action_def, timestamp) VALUES (1, ${now - 30_000})")
            db.execSQL("INSERT INTO action_event (id_action_def, timestamp) VALUES (2, ${now - 25_000})")
            db.execSQL("INSERT INTO action_event (id_action_def, timestamp) VALUES (3, ${now - 20_000})")
            db.execSQL("INSERT INTO action_event (id_action_def, timestamp) VALUES (1, ${now - 10_000})")

            db.setTransactionSuccessful()
            Log.i(TAG, "Successfully seeded action log database with initial commands and events.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed database", e)
        } finally {
            db.endTransaction()
        }
    }
}
