package com.zpi.slowomir.overlay.events

import com.zpi.slowomir.overlay.commands.Command

/**
 * Events emitted by voice recording/recognition colleagues to the [com.zpi.slowomir.overlay.mediator.CommandMediator].
 */
sealed class VoiceEvent {
    object WakeWordDetected : VoiceEvent()
    data class SpeechPartial(val text: String) : VoiceEvent()
    data class SpeechRecognized(val text: String) : VoiceEvent()
    data class ProcessingCommand(val text: String) : VoiceEvent()
    data class CommandInterpreted(val command: Command) : VoiceEvent()
    data class InvalidCommand(val text: String) : VoiceEvent()
}
