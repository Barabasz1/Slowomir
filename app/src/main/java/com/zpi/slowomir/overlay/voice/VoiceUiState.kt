package com.zpi.slowomir.overlay.voice

/**
 * State representing the active voice interaction session for UI observers.
 */
sealed class VoiceUiState {
    object Idle : VoiceUiState()
    object ListeningForWakeWord : VoiceUiState()
    data class ListeningToSpeech(val partialText: String) : VoiceUiState()
    data class ProcessingCommand(val text: String) : VoiceUiState()
    data class CommandExecuted(val resultName: String) : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}
