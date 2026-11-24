package com.zpi.slowomir.overlay.macro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class MacroPlayback(val total: Int, val remaining: Int)

@Singleton
class MacroPlaybackState @Inject constructor() {
    private val _playback = MutableStateFlow<MacroPlayback?>(null)
    val playback: StateFlow<MacroPlayback?> = _playback.asStateFlow()

    val isPlaying: Boolean get() = _playback.value != null

    fun start(total: Int) {
        _playback.value = MacroPlayback(total = total, remaining = total)
    }

    fun update(remaining: Int) {
        _playback.value = _playback.value?.copy(remaining = remaining)
    }

    fun finish() {
        _playback.value = null
    }
}
