package com.zpi.slowomir.ui.screens.HomeScreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.mediator.CommandMediator
import com.zpi.slowomir.overlay.ui.VoiceOverlayManager
import com.zpi.slowomir.overlay.commands.grid.ToggleGridCommand
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import com.zpi.slowomir.overlay.voice.LiveSpeechState
import com.zpi.slowomir.overlay.voice.VoiceSessionManager
import com.zpi.slowomir.overlay.commands.grid.ToggleGridCommand
import com.zpi.slowomir.overlay.voice.OverlayState
import com.zpi.slowomir.overlay.voice.VoiceUiState
import com.zpi.slowomir.overlay.voice.VoskSpeechRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val toggleGridCommandFactory: ToggleGridCommand.Factory,
    private val voiceSessionManager: VoiceSessionManager,
    private val voiceOverlayManager: VoiceOverlayManager,
    private val voskSpeechRecognizer: VoskSpeechRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommandResult?>(null)
    val uiState: StateFlow<CommandResult?> = _uiState.asStateFlow()
    val overlayState: StateFlow<MediatorState> = mediator.mediatorState

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
            val tapCommand = tapCommandFactory.create(x, y)
            val result = mediator.dispatch(tapCommand)
            _uiState.value = result
        }
    }

    fun toggleVoiceOverlay() {
        viewModelScope.launch {
            voiceOverlayManager.toggleOverlay()
        }
    }

    fun simulateSpeechInput(speechText: String) {
        voiceSessionManager.onSpeechResult(speechText, isFinal = false)
    }

    fun simulateSpeechCompletion(input: String) {
        voiceSessionManager.onSpeechResult(input, isFinal = true)
    }

    fun toggleGrid() {
        viewModelScope.launch {
            val toggleGridCommand = toggleGridCommandFactory.create()
            val result = mediator.dispatch(toggleGridCommand)
            _uiState.value = result
        }
    }
}
