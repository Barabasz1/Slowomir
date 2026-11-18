package com.zpi.slowomir.voice.stt

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.voice.VoiceIntent

enum class TextMode { DICTATE, EDIT }

sealed interface VoiceState {
    data object Stopped : VoiceState
    data object KeywordSpotting : VoiceState
    data class Listening(val partialText: String = "") : VoiceState
    data class Dictating(val partialText: String = "") : VoiceState
    data class Editing(val partialText: String = "") : VoiceState
    data class TextProcessing(val rawText: String, val mode: TextMode) : VoiceState
    data class IntentParsing(val rawText: String) : VoiceState
    data class CommandExecuting(
        val intent: VoiceIntent,
        val command: Command,
        val textMode: TextMode? = null,
    ) : VoiceState
    data class Failed(val reason: String) : VoiceState
}
