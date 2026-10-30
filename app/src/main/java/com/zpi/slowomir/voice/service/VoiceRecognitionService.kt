package com.zpi.slowomir.overlay.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.zpi.slowomir.MainActivity
import com.zpi.slowomir.R
import com.zpi.slowomir.overlay.voice.VoskSpeechRecognizer
import com.zpi.slowomir.voice.stt.SttResult
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceInputSink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.zpi.slowomir.overlay.ui.voice.ContactPickerOverlayManager

/**
 * Dedicated Foreground Service responsible for recording audio and running the Vosk
 * offline speech recognizer in compliance with Android 14+ background microphone requirements.
 */
@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    @Inject
    lateinit var recognizer: VoskSpeechRecognizer

    @Inject
    lateinit var sink: VoiceInputSink

    private var listenJob: Job? = null
    private var stopReason: String? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Inject
    lateinit var contactPickerOverlayManager: ContactPickerOverlayManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        contactPickerOverlayManager.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            Log.i(TAG, "Stopping VoiceRecognitionService requested.")
            stopServiceInternal()
            return START_NOT_STICKY
        }

        if (!startForegroundInternal()) {
            Log.e(TAG, "Failed to start foreground service. Stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot start VoiceRecognitionService: RECORD_AUDIO permission is not granted.")
            stopReason = "RECORD_AUDIO permission is not granted"
            stopServiceInternal()
            return START_NOT_STICKY
        }

        if (listenJob?.isActive == true) {
            Log.i(TAG, "VoiceRecognitionService is already running.")
            return START_NOT_STICKY
        }

        stopReason = null
        Log.i(TAG, "Starting VoiceRecognitionService in foreground...")
        sink.accept(VoiceInput.ServiceStarted)
        startVoiceRecognition()

        return START_NOT_STICKY
    }

    private fun startForegroundInternal(): Boolean {
        return try {
            val notification = createNotification()
            val foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while starting foreground service: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting foreground service: ${e.message}", e)
            false
        }
    }

    private fun startVoiceRecognition() {
        listenJob = scope.launch {
            recognizer.initialize().onFailure {
                fail(it)
                return@launch
            }

            sink.accept(VoiceInput.EnableSpeechRecognition)

            recognizer.startListening()
                .catch { fail(it) }
                .collect { result ->
                    when (result) {
                        is SttResult.Error -> fail(result.exception)
                        is SttResult.Final -> sink.accept(VoiceInput.SpeechFinal(result.text))
                        is SttResult.Partial -> sink.accept(VoiceInput.SpeechPartial(result.text))
                    }
                }
        }
    }

    private fun fail(t: Throwable) {
        stopReason = t.message ?: "Speech engine failed"
        stopServiceInternal()
    }

    private fun stopServiceInternal() {
        listenJob?.cancel()
        listenJob = null
        recognizer.stopListening()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.stopListening()
        sink.accept(VoiceInput.ServiceStopped(stopReason))
        scope.cancel()
        Log.i(TAG, "VoiceRecognitionService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Voice Recognition Service",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Background microphone service for voice command recognition"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceRecognitionService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Słowomir")
            .setContentText("Voice command recognition is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    companion object {
        private const val TAG = "VoiceRecognitionService"
        const val ACTION_START = "com.zpi.slowomir.action.START_VOICE_SERVICE"
        const val ACTION_STOP = "com.zpi.slowomir.action.STOP_VOICE_SERVICE"
        private const val CHANNEL_ID = "slowomir_voice_recognition_channel"
        private const val NOTIFICATION_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
