package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * Handles injection of performing media-related actions using the Accessibility API.
 */
class MediaExecutor(private val service: AccessibilityService) {

    private val audioManager: AudioManager?
        get() = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Pauses the media.
     */
    fun pause(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Plays the media.
     */
    fun play(): Boolean {
        return true // TODO: implementation needed
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
     * Skips the media forward.
     */
    fun skipForward(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Skips the media backward.
     */
    fun skipBackward(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Skips to the next track.
     */
    fun nextTrack(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Skips to the previous track.
     */
    fun previousTrack(): Boolean {
        return true // TODO: implementation needed
    }

    companion object {
        private const val TAG = "MediaExecutor"
    }
}
