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
import com.zpi.slowomir.voice.VoiceSessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.zpi.slowomir.overlay.ui.voice.ContactPickerOverlayManager

/**
 * Dedicated Foreground Service responsible for recording audio and running the Vosk
 * offline speech recognizer in compliance with Android 14+ background microphone requirements.
 */
@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    @Inject
    lateinit var voskSpeechRecognizer: VoskSpeechRecognizer

    @Inject
    lateinit var voiceSessionManager: VoiceSessionManager

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot start VoiceRecognitionService: RECORD_AUDIO permission is not granted.")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.i(TAG, "Starting VoiceRecognitionService in foreground...")
        val foregroundStarted = startForegroundInternal()
        if (foregroundStarted) {
            startVoiceRecognition()
        } else {
            Log.e(TAG, "Failed to start foreground service. Stopping.")
            stopSelf()
        }

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
            _isRunning.value = true
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while starting foreground service: ${e.message}", e)
            _isRunning.value = false
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error starting foreground service: ${e.message}", e)
            _isRunning.value = false
            false
        }
    }

    private fun startVoiceRecognition() {
        voskSpeechRecognizer.initialize(
            onReady = {
                val started = voskSpeechRecognizer.startListening(
                    onPartialResult = { text ->
                        voiceSessionManager.onSpeechPartial(text)
                    },
                    onFinalResult = { text ->
                        voiceSessionManager.onSpeechResult(text)
                    },
                    onError = { throwable ->
                        Log.e(TAG, "Voice recognition error: ${throwable.message}", throwable)
                    }
                )

                if (started) {
                    Log.i(TAG, "Voice recognition listener active.")
                } else {
                    Log.e(TAG, "Unable to initialize microphone input. Stopping service.")
                    stopServiceInternal()
                }
            },
            onError = { throwable ->
                Log.e(TAG, "Failed to load speech model: ${throwable.message}", throwable)
                stopServiceInternal()
            }
        )
    }

    private fun stopServiceInternal() {
        voskSpeechRecognizer.stopListening()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        voskSpeechRecognizer.stopListening()
        _isRunning.value = false
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

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

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
