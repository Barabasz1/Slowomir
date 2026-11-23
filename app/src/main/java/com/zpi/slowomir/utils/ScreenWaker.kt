package com.zpi.slowomir.utils

import android.content.Context
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Turns the screen on using [PowerManager].
 */
class ScreenWaker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun isScreenOn(): Boolean = powerManager.isInteractive

    @Suppress("DEPRECATION")
    fun wakeUp(durationMs: Long = DEFAULT_WAKE_DURATION_MS) {
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            WAKE_LOCK_TAG
        ).apply { setReferenceCounted(false) }

        try {
            wakeLock.acquire(durationMs)
            Log.d(TAG, "Screen wake requested for ${durationMs}ms")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing WAKE_LOCK permission", e)
        }
    }

    companion object {
        private const val TAG = "ScreenWaker"
        private const val WAKE_LOCK_TAG = "Slowomir:ScreenWaker"
        private const val DEFAULT_WAKE_DURATION_MS = 10_000L
    }
}
