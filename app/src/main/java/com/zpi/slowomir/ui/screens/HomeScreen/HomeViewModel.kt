package com.zpi.slowomir.ui.screens.HomeScreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zpi.slowomir.kws.KeywordTestController
import com.zpi.slowomir.kws.KeywordUiState
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.grid.ToggleGridCommand
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.voice.VoiceEvent
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.MediatorState
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val toggleGridCommandFactory: ToggleGridCommand.Factory,
    private val voiceSessionManager: VoiceSessionManager,
    private val voiceOverlayManager: VoiceOverlayManager,
    private val keywordTestController: KeywordTestController,
    private val contactPickerOverlayManager: ContactPickerOverlayManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommandResult?>(null)
    val uiState: StateFlow<CommandResult?> = _uiState.asStateFlow()
    val overlayState: StateFlow<MediatorState> = mediator.mediatorState

    val voiceState: StateFlow<VoiceState> = voiceSessionManager.state

    val isVoiceRunning: StateFlow<Boolean> = voiceState
        .map { it !is VoiceState.Stopped && it !is VoiceState.Failed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val kwsState: StateFlow<KeywordUiState> = keywordTestController.state

    init {
        contactPickerOverlayManager.start()
    }

    fun startKws() = keywordTestController.start()

    fun stopKws() = keywordTestController.stop()

    fun onKwsPermissionDenied() = keywordTestController.onPermissionDenied()

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
        voiceSessionManager.accept(VoiceInput.SpeechPartial(speechText))
    }

    fun simulateSpeechCompletion(input: String) {
        voiceSessionManager.accept(VoiceInput.SpeechFinal(input))
    }

    fun toggleGrid() {
        viewModelScope.launch {
            val toggleGridCommand = toggleGridCommandFactory.create()
            val result = mediator.dispatch(toggleGridCommand)
            _uiState.value = result
        }
    }
}
