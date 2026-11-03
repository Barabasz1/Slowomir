package com.zpi.slowomir.kws

import android.content.Context
import com.zpi.slowomir.overlay.services.VoiceRecognitionService
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.stt.VoiceState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordTestController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceSessionManager: VoiceSessionManager,
    private val kws: KeywordSpotter,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(KeywordUiState())
    val state: StateFlow<KeywordUiState> = _state.asStateFlow()

    private val isVoiceServiceRunning = voiceSessionManager.state
        .map { it !is VoiceState.Stopped && it !is VoiceState.Failed }
        .distinctUntilChanged()

    private var testJob: Job? = null
    @Volatile private var switchingFromService = false

    init {
        scope.launch {
            isVoiceServiceRunning.collect { running ->
                if (running && !switchingFromService && _state.value.isListening) stop()
            }
        }
    }

    fun start() {
        if (_state.value.isListening) return
        _state.value = KeywordUiState(isListening = true)

        val previous = testJob
        testJob = scope.launch {
            previous?.cancelAndJoin()

            if (isVoiceServiceRunning.first()) {
                switchingFromService = true
                try {
                    VoiceRecognitionService.stop(context)
                    withTimeoutOrNull(SERVICE_STOP_TIMEOUT_MS) {
                        isVoiceServiceRunning.first { !it }
                    }
                    delay(MIC_RELEASE_DELAY_MS)
                } finally {
                    switchingFromService = false
                }
            }

            runTest()
        }
    }

    fun stop() {
        testJob?.cancel()
        _state.update { it.copy(isListening = false) }
    }

    fun onPermissionDenied() {
        _state.update { it.copy(isListening = false, error = KeywordError.PERMISSION_DENIED) }
    }

    private suspend fun runTest() {
        kws.initialize().onFailure {
            _state.update { s -> s.copy(isListening = false, error = KeywordError.MODEL_LOAD_FAILED) }
            return
        }
        try {
            _state.update { it.copy(isListening = true, error = null) }
            kws.startListening().collect { result ->
                when (result) {
                    KwsResult.Detected ->
                        _state.update { it.copy(detections = it.detections + KEYWORD_LABEL) }
                    is KwsResult.Error ->
                        _state.update { it.copy(isListening = false, error = result.error) }
                }
            }
        } finally {
            kws.stopListening()
            _state.update { it.copy(isListening = false) }
            withContext(NonCancellable + Dispatchers.IO) { kws.release() }
        }
    }

    private companion object {
        const val KEYWORD_LABEL = "Słowomir"
        const val SERVICE_STOP_TIMEOUT_MS = 2_000L
        const val MIC_RELEASE_DELAY_MS = 300L
    }
}
