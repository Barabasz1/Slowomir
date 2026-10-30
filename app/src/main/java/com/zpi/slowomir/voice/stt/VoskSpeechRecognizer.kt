package com.zpi.slowomir.overlay.voice

import android.content.Context
import android.util.Log
import com.zpi.slowomir.voice.stt.SpeechRecognizer
import com.zpi.slowomir.voice.stt.SttResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages the Vosk speech recognition lifecycle, including offline model unpacking,
 * recognizer initialization, and audio recording via [SpeechService].
 */
@Singleton
class VoskSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechRecognizer {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null

    @Volatile
    private var isListening = false

    @Volatile
    private var lastDispatchedText: String? = null

    @Volatile
    private var lastDispatchedTime: Long = 0L

    /**
     * Initializes the Vosk speech model asynchronously by unpacking assets if necessary.
     */
    override suspend fun initialize(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (model != null && recognizer != null) {
            continuation.resume(Result.success(Unit))
            return@suspendCancellableCoroutine
        }

        Log.i(TAG, "Unpacking Vosk model from assets...")

        StorageService.unpack(
            context,
            ASSET_MODEL_PATH,
            STORAGE_TARGET_NAME,
            { loadedModel ->
                try {
                    model = loadedModel
                    recognizer = Recognizer(loadedModel, SAMPLE_RATE)
                    Log.i(TAG, "Vosk model and recognizer initialized successfully.")
                    continuation.resume(Result.success(Unit))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create Recognizer from loaded model", e)
                    continuation.resume(Result.failure(e))
                }
            },
            { exception: IOException ->
                Log.e(TAG, "Failed to unpack Vosk model", exception)
                continuation.resume(Result.failure(exception))
            }
        )
    }

    /**
     * Starts listening for voice input via the device microphone.
     */
    override fun startListening(): Flow<SttResult> = callbackFlow {
        if (isListening) {
            Log.w(TAG, "SpeechService is already listening.")
            close()
            return@callbackFlow
        }

        val currentRecognizer = recognizer
        if (currentRecognizer == null) {
            Log.e(TAG, "Cannot start listening: Recognizer is not initialized.")
            trySend(SttResult.Error(IllegalStateException("Recognizer is not initialized")))
            close()
            return@callbackFlow
        }

        try {
            // Clean up any previously remaining speechService instance
            speechService?.stop()
            speechService?.shutdown()
            speechService = null

            // Reset recognizer state to start fresh
            currentRecognizer.reset()

            val service = SpeechService(currentRecognizer, SAMPLE_RATE)
            this@VoskSpeechRecognizer.speechService = service

            val started = service.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    extractAndEmitPartial(hypothesis) {
                        trySend(SttResult.Partial(it))
                    }
                }

                override fun onResult(hypothesis: String?) {
                    extractAndDispatchFinal(hypothesis) {
                        trySend(SttResult.Final(it))
                    }
                }

                override fun onFinalResult(hypothesis: String?) {
                    extractAndDispatchFinal(hypothesis) {
                        trySend(SttResult.Final(it))
                    }
                }

                override fun onError(exception: Exception?) {
                    trySend(SttResult.Error(exception ?: IllegalStateException("Unknown error")))
                }

                override fun onTimeout() {
                    Log.d(TAG, "Recognition timed out.")
                    close()
                }
            })

            if (!started) {
                Log.e(TAG, "SpeechService.startListening returned false. Releasing resources.")
                service.shutdown()
                this@VoskSpeechRecognizer.speechService = null
                isListening = false
                trySend(SttResult.Error(IllegalStateException("Failed to start audio listening thread")))
                return@callbackFlow
            }

            isListening = true
            Log.i(TAG, "SpeechService started listening successfully.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechService", e)
            speechService?.shutdown()
            speechService = null
            isListening = false
            trySend(SttResult.Error(e))
        }

        awaitClose {
            Log.i(TAG, "Cleaning up SpeechService via flow cancellation.")
            stopListening()
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
            Log.i(TAG, "SpeechService stopped successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping SpeechService", e)
        } finally {
            speechService = null
            isListening = false
        }
    }

    private fun extractAndEmitPartial(jsonString: String?, callback: (String) -> Unit) {
        if (jsonString.isNullOrBlank()) return

        try {
            val jsonObject = JSONObject(jsonString)
            val partial = jsonObject.optString("partial", "").trim()
            if (partial.isNotBlank()) {
                Log.d(TAG, "Partial speech: '$partial'")
                callback(partial)
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
