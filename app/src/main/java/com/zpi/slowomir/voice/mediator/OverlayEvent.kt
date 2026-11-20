package com.zpi.slowomir.voice.mediator

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.voice.OverlayError
import com.zpi.slowomir.voice.VoiceIntent
import com.zpi.slowomir.voice.stt.TextMode

/**
 * Events emitted by voice recording/recognition colleagues to the [CommandMediator].
 */
sealed interface OverlayEvent {
    object WakeWordDetected : OverlayEvent
    data class SpeechPartial(val text: String, val mode: TextMode? = null) : OverlayEvent
    data class SpeechRecognized(val text: String, val mode: TextMode? = null) : OverlayEvent
    object SessionIdle : OverlayEvent
    data class ProcessingCommand(val text: String) : OverlayEvent
    data class IntentParsed(val intent: VoiceIntent) : OverlayEvent
    data class CommandExecuted(val command: Command, val result: CommandResult) : OverlayEvent
    data class InvalidCommand(val text: String) : OverlayEvent
    object AwaitingSelection : OverlayEvent
    data class CommandRejected(val error: OverlayError) : OverlayEvent
}