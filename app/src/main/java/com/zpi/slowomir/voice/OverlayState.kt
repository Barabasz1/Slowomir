package com.zpi.slowomir.voice

import com.zpi.slowomir.voice.selection.SelectionFailure

/**
 * State representing the active voice interaction session for UI observers.
 */
sealed class OverlayState {
    object Idle : OverlayState()
    object ListeningForKeyword : OverlayState()
    object ListeningForCommand : OverlayState()
    data class ListeningToSpeech(val partialText: String) : OverlayState()
    data class Dictating(val text: String) : OverlayState()
    data class Editing(val text: String) : OverlayState()

    data class ProcessingCommand(val text: String) : OverlayState()
    data class CommandExecuted(val commandName: String, val intent: VoiceIntent?) : OverlayState()
    data class Error(val error: OverlayError) : OverlayState()

    val isHidden: Boolean get() = this is Idle || this is ListeningForKeyword
    val isResult: Boolean get() = this is CommandExecuted || this is Error
}

sealed interface OverlayError {
    data class InvalidCommand(val text: String) : OverlayError
    data class CommandFailed(val commandName: String, val reason: String) : OverlayError
    data class InvalidContext(val reason: String) : OverlayError
    data class SelectionFailed(val failure: SelectionFailure) : OverlayError
    data object TextInsertFailed : OverlayError
}
