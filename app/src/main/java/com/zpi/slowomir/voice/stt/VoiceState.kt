package com.zpi.slowomir.voice.stt

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.voice.VoiceIntent

sealed interface VoiceState {
    data object Stopped : VoiceState
    data object KeywordSpotting : VoiceState
    data class Listening(val partialText: String = "") : VoiceState
    data class IntentParsing(val rawText: String) : VoiceState
    data class CommandExecuting(val intent: VoiceIntent, val command: Command) : VoiceState
    data class Failed(val reason: String) : VoiceState
}
