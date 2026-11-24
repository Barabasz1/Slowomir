package com.zpi.slowomir.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reports whether any app is currently playing media.
 */
class MediaPlaybackMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Emits `true` when media starts playing and `false` when it stops.
     * Changes are debounced for [SETTLE_MS].
     */
    @OptIn(FlowPreview::class)
    val isPlaying: Flow<Boolean> = callbackFlow {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val callback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                trySend(configs.any { it.isMedia() })
            }
        }
        audioManager.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        trySend(audioManager.activePlaybackConfigurations.any { it.isMedia() })

        awaitClose { audioManager.unregisterAudioPlaybackCallback(callback) }
    }
        .distinctUntilChanged()
        .debounce(SETTLE_MS.milliseconds)
        .distinctUntilChanged()

    /**
     * Returns whether media is playing right now, without the settling delay of [isPlaying].
     */
    fun isPlayingNow(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.activePlaybackConfigurations.any { it.isMedia() }
    }

    private fun AudioPlaybackConfiguration.isMedia(): Boolean =
        audioAttributes.usage in MEDIA_USAGES

    private companion object {
        const val SETTLE_MS = 1_000L

        val MEDIA_USAGES = setOf(
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_GAME,
            AudioAttributes.USAGE_UNKNOWN,
        )
    }
}
