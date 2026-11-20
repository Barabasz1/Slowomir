package com.zpi.slowomir.voice

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
    data class CommandExecuted(val resultName: String) : OverlayState()
    data class Error(val message: String) : OverlayState()

    val isHidden: Boolean get() = this is Idle || this is ListeningForKeyword
    val isResult: Boolean get() = this is CommandExecuted || this is Error
}
