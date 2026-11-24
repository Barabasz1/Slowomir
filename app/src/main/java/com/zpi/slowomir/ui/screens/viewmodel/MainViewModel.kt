package com.zpi.slowomir.ui.screens.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.overlay.macro.MacroRepository
import com.zpi.slowomir.overlay.macro.MacroScreenState
import com.zpi.slowomir.overlay.macro.MacroSupplier
import com.zpi.slowomir.overlay.ui.macro.MacroEntry
import com.zpi.slowomir.overlay.ui.macro.appLabelResolver
import com.zpi.slowomir.overlay.ui.confirmation.ConfirmationOverlayManager
import com.zpi.slowomir.overlay.ui.macro.MacroRecordingOverlayManager
import com.zpi.slowomir.overlay.commands.grid.GridToggleCommand
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import com.zpi.slowomir.overlay.ui.grid.GridType
import com.zpi.slowomir.overlay.ui.voice.PickerOverlayManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val gridToggleCommandFactory: GridToggleCommand.Factory,
    private val voiceSessionManager: VoiceSessionManager,
    private val voiceOverlayManager: VoiceOverlayManager,
    pickerOverlayManager: PickerOverlayManager, // injected so it starts observing selections
    macroRecordingOverlayManager: MacroRecordingOverlayManager, // injected so it starts observing the recorder
    confirmationOverlayManager: ConfirmationOverlayManager, // injected so it starts observing questions
    private val macroRepository: MacroRepository,
    private val macroSupplier: MacroSupplier,
    private val macroScreenState: MacroScreenState,
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

    val gridRows: StateFlow<Int> = preferencesManager.gridRows
    val gridCols: StateFlow<Int> = preferencesManager.gridCols
    val gridType: StateFlow<GridType> = preferencesManager.gridType
    val invertedControls: StateFlow<Boolean> = preferencesManager.invertedControls

    private val appLabel = appLabelResolver(context)

    val recordedMacros: StateFlow<List<MacroEntry>> = macroRepository.recordedMacros
        .map { macros -> macros.map { MacroEntry(it, appLabel) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val generatedMacros: StateFlow<List<MacroEntry>> = macroRepository.generatedMacros
        .map { macros -> macros.map { MacroEntry(it, appLabel) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMacrosScreenVisible(visible: Boolean) {
        macroScreenState.setVisible(visible)
        if (visible) refreshGeneratedMacros()
    }

    fun refreshGeneratedMacros() {
        viewModelScope.launch(Dispatchers.IO) { macroRepository.reloadGeneratedMacros() }
    }

    fun keepMacro(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val index = position - macroRepository.recordedMacros.value.size - 1
            if (macroRepository.keepGeneratedMacro(index)) macroSupplier.refresh()
        }
    }

    fun deleteMacro(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (macroRepository.deleteRecordedMacro(position - 1)) {
                macroSupplier.refresh()
                macroRepository.reloadGeneratedMacros()
            }
        }
    }

    fun setGridRows(rows: Int) = preferencesManager.setGridRows(rows)
    fun setGridCols(cols: Int) = preferencesManager.setGridCols(cols)
    fun setGridType(type: GridType) = preferencesManager.setGridType(type)

    fun setInvertedControls(inverted: Boolean) = preferencesManager.setInvertedControls(inverted)

    init {
        viewModelScope.launch(Dispatchers.IO) { macroRepository.reloadRecordedMacros() }
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
            val toggleGridCommand = gridToggleCommandFactory.create()
            val result = mediator.dispatch(toggleGridCommand)
            _uiState.value = result
        }
    }
}
