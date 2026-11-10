package com.zpi.slowomir.ui.screens.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.grid.ToggleGridCommand
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import com.zpi.slowomir.overlay.ui.voice.ContactPickerOverlayManager
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.MediatorState
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceState
import com.zpi.slowomir.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val toggleGridCommandFactory: ToggleGridCommand.Factory,
    private val voiceSessionManager: VoiceSessionManager,
    private val voiceOverlayManager: VoiceOverlayManager,
    contactPickerOverlayManager: ContactPickerOverlayManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommandResult?>(null)
    val uiState: StateFlow<CommandResult?> = _uiState.asStateFlow()
    val overlayState: StateFlow<MediatorState> = mediator.mediatorState

    val voiceState: StateFlow<VoiceState> = voiceSessionManager.state

    val isVoiceRunning: StateFlow<Boolean> = voiceState
        .map { it !is VoiceState.Stopped && it !is VoiceState.Failed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isOverlayEnabled: StateFlow<Boolean> = preferencesManager.isOverlayEnabled
    val isOnboardingCompleted: StateFlow<Boolean> = preferencesManager.isOnboardingCompleted
    val showIntroHighlight: StateFlow<Boolean> = preferencesManager.showIntroHighlight

    init {
        contactPickerOverlayManager.start()
        viewModelScope.launch {
            if (isOverlayEnabled.value) {
                voiceOverlayManager.showOverlay()
            }
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        preferencesManager.setOverlayEnabled(enabled)
        viewModelScope.launch {
            if (enabled) {
                voiceOverlayManager.showOverlay()
            } else {
                voiceOverlayManager.hideOverlay()
            }
        }
    }

    fun completeOnboarding() {
        preferencesManager.setOnboardingCompleted(true)
    }

    fun dismissIntroHighlight() {
        preferencesManager.setIntroHighlightShown(false)
    }

    fun startVoiceService() {
        VoiceRecognitionService.start(context)
        if (isOverlayEnabled.value) {
            viewModelScope.launch {
                voiceOverlayManager.showOverlay()
            }
        }
    }

    fun stopVoiceService() {
        VoiceRecognitionService.stop(context)
    }

    fun executeTapAt(x: Float, y: Float) {
        viewModelScope.launch {
            val tapCommand = tapCommandFactory.create(x, y)
            val result = mediator.dispatch(tapCommand)
            _uiState.value = result
        }
    }

    fun simulateSpeechInput(speechText: String) {
        voiceSessionManager.accept(VoiceInput.SpeechPartial(speechText))
    }

    fun simulateWakeWord() {
        voiceSessionManager.accept(VoiceInput.KeywordDetected)
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
