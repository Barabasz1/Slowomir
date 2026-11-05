package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.Command

/**
 * Events emitted by voice recording/recognition colleagues to the [com.zpi.slowomir.voice.mediator.CommandMediator].
 */
sealed interface OverlayEvent {
    object WakeWordDetected : OverlayEvent
    data class SpeechPartial(val text: String) : OverlayEvent
    data class SpeechRecognized(val text: String) : OverlayEvent
    data class ProcessingCommand(val text: String) : OverlayEvent
    data class IntentParsed(val intent: VoiceIntent) : OverlayEvent
    data class CommandInterpreted(val command: Command) : OverlayEvent
    data class InvalidCommand(val text: String) : OverlayEvent

    /** The contact picker was closed (a contact was chosen, cancelled or timed out). */
    object SelectionCleared : OverlayEvent
}