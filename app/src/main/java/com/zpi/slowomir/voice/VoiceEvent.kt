package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.Command

/**
 * Events emitted by voice recording/recognition colleagues to the [com.zpi.slowomir.voice.mediator.CommandMediator].
 */
sealed class VoiceEvent {
    object WakeWordDetected : VoiceEvent()
    data class SpeechPartial(val text: String) : VoiceEvent()
    data class SpeechRecognized(val text: String) : VoiceEvent()
    data class ProcessingCommand(val text: String) : VoiceEvent()
    data class IntentParsed(val intent: VoiceIntent) : VoiceEvent()
    data class CommandInterpreted(val command: Command) : VoiceEvent()
    data class InvalidCommand(val text: String) : VoiceEvent()

    /** The contact picker was closed (a contact was chosen, cancelled or timed out). */
    object SelectionCleared : VoiceEvent()
}