package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService

/**
 * Handles injection of performing media-related actions using the Accessibility API.
 */
class MediaExecutor(service: AccessibilityService) {
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
        return true // TODO: implementation needed
    }

    /**
     * Turns the media volume down.
     */
    fun volumeDown(): Boolean {
        return true // TODO: implementation needed
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
}