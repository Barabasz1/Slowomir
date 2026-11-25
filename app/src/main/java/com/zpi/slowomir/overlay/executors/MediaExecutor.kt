package com.zpi.slowomir.overlay.executors

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.zpi.slowomir.overlay.services.MediaListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles media-related actions.
 */
@Singleton
class MediaExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val audioManager: AudioManager?
        get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private fun activeController(): MediaController? {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return null
        return try {
            val sessions = manager.getActiveSessions(ComponentName(context, MediaListenerService::class.java))
            sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification access not granted, using media keys (${e.message})")
            null
        }
    }

    private fun sendMediaKey(keyCode: Int): Boolean {
        val am = audioManager ?: run {
            Log.w(TAG, "AudioManager not available, cannot send media key")
            return false
        }
        return try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send media key $keyCode", e)
            false
        }
    }

    /**
     * Pauses the media.
     */
    fun pause(): Boolean {
        val controller = activeController()
        if (controller != null) {
            controller.transportControls.pause()
            return true
        }
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    /**
     * Plays the media.
     */
    fun play(): Boolean {
        val controller = activeController()
        if (controller != null) {
            controller.transportControls.play()
            return true
        }
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    /**
     * Turns the media volume up.
     */
    fun volumeUp(): Boolean {
        val am = audioManager ?: run {
            Log.w(TAG, "AudioManager not available, cannot raise volume")
            return false
        }
        return try {
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            Log.i(TAG, "Media volume raised")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to raise media volume", e)
            false
        }
    }

    /**
     * Turns the media volume down.
     */
    fun volumeDown(): Boolean {
        val am = audioManager ?: run {
            Log.w(TAG, "AudioManager not available, cannot lower volume")
            return false
        }
        return try {
            am.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            Log.i(TAG, "Media volume lowered")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lower media volume", e)
            false
        }
    }

    /**
     * Skips the media forward by [seconds] (defaults to [SEEK_STEP_MS]).
     */
    fun skipForward(seconds: Int? = null): Boolean =
        seekBy(stepMs(seconds), KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

    /**
     * Skips the media backward by [seconds] (defaults to [SEEK_STEP_MS]).
     */
    fun skipBackward(seconds: Int? = null): Boolean =
        seekBy(-stepMs(seconds), KeyEvent.KEYCODE_MEDIA_REWIND)

    private fun stepMs(seconds: Int?): Long =
        if (seconds != null && seconds > 0) seconds * 1_000L else SEEK_STEP_MS

    /**
     * Skips to the next track.
     */
    fun nextTrack(): Boolean {
        val controller = activeController()
        if (controller != null) {
            controller.transportControls.skipToNext()
            return true
        }
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    /**
     * Skips to the previous track.
     */
    fun previousTrack(): Boolean {
        val controller = activeController()
        if (controller != null) {
            controller.transportControls.skipToPrevious()
            return true
        }
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun seekBy(deltaMs: Long, fallbackKey: Int): Boolean {
        val controller = activeController()
        val state = controller?.playbackState
        if (controller == null || state == null ||
            state.actions and PlaybackState.ACTION_SEEK_TO == 0L
        ) {
            return sendMediaKey(fallbackKey)
        }

        var position = state.position
        if (state.state == PlaybackState.STATE_PLAYING) {
            val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            position += (elapsed * state.playbackSpeed).toLong()
        }

        var target = (position + deltaMs).coerceAtLeast(0L)
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (duration > 0L) target = target.coerceAtMost(duration)

        controller.transportControls.seekTo(target)
        return true
    }

    companion object {
        private const val TAG = "MediaExecutor"

        const val SEEK_STEP_MS = 5_000L
    }
}
