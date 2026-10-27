package com.zpi.slowomir.overlay.voice

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State representing raw live speech recognition output.
 *
 * @param text The transcribed text from Vosk.
 * @param isPartial True if this is an interim hypothesis, false if finalized.
 * @param statusMessage Informative status message displayed when speech is empty.
 */
data class LiveSpeechState(
    val text: String = "",
    val isPartial: Boolean = false,
    val statusMessage: String? = null
)

/**
 * Manages the Vosk speech recognition lifecycle, including offline model unpacking,
 * recognizer initialization, and audio recording via [SpeechService].
 *
 * Exposes a direct [liveTranscript] stream for UI feedback (including partial results)
 * while only dispatching finalized results to the command execution pipeline.
 */
@Singleton
class VoskSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null

    private val _liveTranscript = MutableStateFlow(LiveSpeechState())
    val liveTranscript: StateFlow<LiveSpeechState> = _liveTranscript.asStateFlow()

    @Volatile
    private var isInitializing = false

    @Volatile
    private var isListening = false

    @Volatile
    private var lastDispatchedText: String? = null

    @Volatile
    private var lastDispatchedTime: Long = 0L

    /**
     * Initializes the Vosk speech model asynchronously by unpacking assets if necessary.
     *
     * @param onReady Invoked when the model and recognizer are ready.
     * @param onError Invoked if model unpacking or initialization fails.
     */
    @Synchronized
    fun initialize(onReady: () -> Unit, onError: (Throwable) -> Unit) {
        if (model != null && recognizer != null) {
            onReady()
            return
        }

        if (isInitializing) {
            Log.d(TAG, "Initialization already in progress.")
            return
        }

        isInitializing = true
        _liveTranscript.value = LiveSpeechState(statusMessage = "Initializing stt...")
        Log.i(TAG, "Unpacking Vosk model from assets...")

        StorageService.unpack(
            context,
            ASSET_MODEL_PATH,
            STORAGE_TARGET_NAME,
            { loadedModel ->
                try {
                    model = loadedModel
                    recognizer = Recognizer(loadedModel, SAMPLE_RATE)
                    isInitializing = false
                    Log.i(TAG, "Vosk model and recognizer initialized successfully.")
                    onReady()
                } catch (e: Exception) {
                    isInitializing = false
                    Log.e(TAG, "Failed to create Recognizer from loaded model", e)
                    _liveTranscript.value = LiveSpeechState(statusMessage = "Recognizer error: ${e.message}")
                    onError(e)
                }
            },
            { exception: IOException ->
                isInitializing = false
                Log.e(TAG, "Failed to unpack Vosk model", exception)
                _liveTranscript.value = LiveSpeechState(statusMessage = "Error loading model: ${exception.message}")
                onError(exception)
            }
        )
    }

    /**
     * Starts listening for voice input via the device microphone.
     *
     * @param onFinalResult Callback invoked when a complete speech utterance is recognized.
     * @param onError Callback invoked when an error occurs during speech capture or recognition.
     * @return True if listening was successfully started, false otherwise.
     */
    @Synchronized
    fun startListening(onFinalResult: (String) -> Unit, onError: (Throwable) -> Unit): Boolean {
        if (isListening) {
            Log.w(TAG, "SpeechService is already listening.")
            return true
        }

        val currentRecognizer = recognizer
        if (currentRecognizer == null) {
            Log.e(TAG, "Cannot start listening: Recognizer is not initialized.")
            _liveTranscript.value = LiveSpeechState(statusMessage = "Error: model is not ready yet.")
            return false
        }

        return try {
            // Clean up any previously remaining speechService instance
            speechService?.stop()
            speechService?.shutdown()
            speechService = null

            // Reset recognizer state to start fresh
            currentRecognizer.reset()

            val service = SpeechService(currentRecognizer, SAMPLE_RATE)
            this.speechService = service

            val started = service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    extractAndEmitPartial(hypothesis)
                }

                override fun onResult(hypothesis: String?) {
                    extractAndDispatchFinal(hypothesis, onFinalResult)
                }

                override fun onFinalResult(hypothesis: String?) {
                    extractAndDispatchFinal(hypothesis, onFinalResult)
                }

                override fun onError(exception: Exception?) {
                    Log.e(TAG, "Recognition error occurred", exception)
                    _liveTranscript.value = LiveSpeechState(statusMessage = "Recognition error: ${exception?.message}")
                    exception?.let(onError)
                }

                override fun onTimeout() {
                    Log.d(TAG, "Recognition timed out.")
                }
            })

            if (!started) {
                Log.e(TAG, "SpeechService.startListening returned false. Releasing resources.")
                service.shutdown()
                this.speechService = null
                isListening = false
                _liveTranscript.value = LiveSpeechState(statusMessage = "Failed to start audio listening thread.")
                return false
            }

            isListening = true
            _liveTranscript.value = LiveSpeechState(statusMessage = "Listening...")
            Log.i(TAG, "SpeechService started listening successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechService", e)
            speechService?.shutdown()
            speechService = null
            isListening = false
            _liveTranscript.value = LiveSpeechState(statusMessage = "Microphone error: ${e.message}")
            onError(e)
            false
        }
    }

    /**
     * Stops the active speech recording service and releases AudioRecord resources.
     */
    @Synchronized
    fun stopListening() {
        if (!isListening && speechService == null) return

        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
            isListening = false
            _liveTranscript.value = LiveSpeechState(statusMessage = "Speech recognition is stopped.")
            Log.i(TAG, "SpeechService stopped successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping SpeechService", e)
            speechService = null
            isListening = false
        }
    }

    fun isListeningActive(): Boolean = isListening

    fun isModelReady(): Boolean = model != null && recognizer != null

    private fun extractAndEmitPartial(jsonString: String?) {
        if (jsonString.isNullOrBlank()) return

        try {
            val jsonObject = JSONObject(jsonString)
            val partial = jsonObject.optString("partial", "").trim()
            if (partial.isNotBlank()) {
                Log.d(TAG, "Partial speech: '$partial'")
                _liveTranscript.value = LiveSpeechState(text = partial, isPartial = true, statusMessage = "Listening...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse partial result JSON", e)
        }
    }

    private fun extractAndDispatchFinal(jsonString: String?, callback: (String) -> Unit) {
        if (jsonString.isNullOrBlank()) return

        try {
            val jsonObject = JSONObject(jsonString)
            val text = jsonObject.optString("text", "").trim()
            if (text.isNotBlank()) {
                val now = System.currentTimeMillis()
                if (text == lastDispatchedText && (now - lastDispatchedTime) < DUPLICATE_SUPPRESSION_WINDOW_MS) {
                    Log.d(TAG, "Suppressed duplicate dispatch of text: '$text'")
                    return
                }
                lastDispatchedText = text
                lastDispatchedTime = now

                Log.i(TAG, "Recognized final text: '$text'")
                _liveTranscript.value = LiveSpeechState(text = text, isPartial = false, statusMessage = "Recognized")
                callback(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recognition result JSON: $jsonString", e)
        }
    }

    companion object {
        private const val TAG = "VoskSpeechRecognizer"
        private const val ASSET_MODEL_PATH = "model-pl"
        private const val STORAGE_TARGET_NAME = "model"
        private const val SAMPLE_RATE = 16000.0f
        private const val DUPLICATE_SUPPRESSION_WINDOW_MS = 1000L
    }
}
