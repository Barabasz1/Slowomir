package com.zpi.slowomir.ui.screens.HomeScreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.grid.ToggleGridCommand
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import com.zpi.slowomir.overlay.voice.LiveSpeechState
import com.zpi.slowomir.overlay.voice.VoiceSessionManager
import com.zpi.slowomir.overlay.voice.VoiceUiState
import com.zpi.slowomir.overlay.voice.VoskSpeechRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the Home screen state, command invocation, and voice service controls.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tapCommandFactory: TapCommand.Factory,
    private val toggleGridCommandFactory: ToggleGridCommand.Factory,
    private val voiceSessionManager: VoiceSessionManager,
    private val voskSpeechRecognizer: VoskSpeechRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommandResult?>(null)
    val uiState: StateFlow<CommandResult?> = _uiState.asStateFlow()

    val voiceUiState: StateFlow<VoiceUiState> = voiceSessionManager.uiState
    val isVoiceServiceRunning: StateFlow<Boolean> = VoiceRecognitionService.isRunning
    val liveSpeechState: StateFlow<LiveSpeechState> = voskSpeechRecognizer.liveTranscript

    fun startVoiceService(context: Context) {
        VoiceRecognitionService.start(context)
    }

    fun stopVoiceService(context: Context) {
        VoiceRecognitionService.stop(context)
    }

    fun executeTapAt(x: Float, y: Float) {
        viewModelScope.launch {
            val tapCommand: Command = tapCommandFactory.create(x, y)
            val context = OverlayContext("", false)

            if (!tapCommand.canExecute(context)) {
                _uiState.value = CommandResult.InvalidContext("Target ($x, $y) out of screen bounds")
                return@launch
            }

            val result = tapCommand.execute()
            _uiState.value = result
        }
    }

    fun simulateVoiceInput(input: String) {
        voiceSessionManager.onSpeechResult(input, isFinal = true)
    }

    fun toggleGrid() {
        viewModelScope.launch {
            val command: Command = toggleGridCommandFactory.create()
            command.execute()
        }
    }
}
