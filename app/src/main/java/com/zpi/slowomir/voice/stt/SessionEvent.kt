package com.zpi.slowomir.voice.stt

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.voice.VoiceIntent
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionEvent

sealed interface VoiceInput : SessionEvent {
    data object ServiceStarted : VoiceInput
    data class ServiceStopped(val error: String? = null) : VoiceInput
    data object KeywordDetected : VoiceInput
    data class SpeechPartial(val text: String) : VoiceInput
    data class SpeechFinal(val text: String) : VoiceInput
    data object EnableKwsMode : VoiceInput
    data object EnableSpeechRecognition : VoiceInput
}

internal sealed interface InternalEvent : SessionEvent {
    data class IntentParsed(val intent: VoiceIntent, val command: Command) : InternalEvent
    data class IntentFailed(val text: String) : InternalEvent
    data class CommandFinished(val result: CommandResult, val returnToKws: Boolean = false) : InternalEvent
    data class SelectionPicked(val index: Int) : InternalEvent
    data object SelectionCancelled : InternalEvent
}

interface VoiceInputSink { fun accept(input: VoiceInput) }
interface VoiceStateSource { val state: StateFlow<VoiceState> }
