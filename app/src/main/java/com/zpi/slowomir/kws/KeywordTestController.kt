package com.zpi.slowomir.kws

import android.content.Context
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordTestController @Inject constructor(
    @ApplicationContext private val context: Context
) : KeywordSpotterManager.Callback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(KeywordUiState())
    val state: StateFlow<KeywordUiState> = _state.asStateFlow()

    private val manager by lazy { KeywordSpotterManager(context, this) }

    private var startJob: Job? = null
    @Volatile private var switchingFromVosk = false

    init {
        scope.launch {
            VoiceRecognitionService.isRunning.collect { running ->
                if (running && !switchingFromVosk && _state.value.isListening) stop()
            }
        }
    }

    fun start() {
        if (_state.value.isListening) return
        _state.value = KeywordUiState(isListening = true)

        startJob?.cancel()
        startJob = scope.launch {
            if (VoiceRecognitionService.isRunning.value) {
                switchingFromVosk = true
                try {
                    VoiceRecognitionService.stop(context)
                    withTimeoutOrNull(SERVICE_STOP_TIMEOUT_MS) {
                        VoiceRecognitionService.isRunning.first { !it }
                    }
                    delay(MIC_RELEASE_DELAY_MS)
                } finally {
                    switchingFromVosk = false
                }
            }
            if (_state.value.isListening) manager.start()
        }
    }

    fun stop() {
        startJob?.cancel()
        if (!_state.value.isListening) return
        _state.update { it.copy(isListening = false) }
        manager.stop()
    }

    fun onPermissionDenied() {
        _state.update { it.copy(error = KeywordError.PERMISSION_DENIED) }
    }

    override fun onListeningStarted() {
        _state.update { it.copy(isListening = true, error = null) }
    }

    override fun onKeywordDetected() {
        _state.update { it.copy(detections = it.detections + KEYWORD_LABEL) }
    }

    override fun onListeningStopped() {
        _state.update { it.copy(isListening = false) }
    }

    override fun onError(error: KeywordError) {
        _state.update { it.copy(isListening = false, error = error) }
    }

    private companion object {
        const val KEYWORD_LABEL = "Słowomir"
        const val SERVICE_STOP_TIMEOUT_MS = 2_000L
        const val MIC_RELEASE_DELAY_MS = 300L
    }
}