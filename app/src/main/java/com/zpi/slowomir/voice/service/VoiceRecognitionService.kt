package com.zpi.slowomir.overlay.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.zpi.slowomir.MainActivity
import com.zpi.slowomir.R
import com.zpi.slowomir.kws.KeywordSpotter
import com.zpi.slowomir.kws.KwsResult
import com.zpi.slowomir.overlay.executors.MediaExecutor
import com.zpi.slowomir.utils.MediaPlaybackMonitor
import com.zpi.slowomir.utils.ScreenWaker
import com.zpi.slowomir.voice.stt.SpeechRecognizer
import com.zpi.slowomir.voice.stt.SttResult
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceInputSink
import com.zpi.slowomir.voice.stt.VoiceState
import com.zpi.slowomir.voice.stt.VoiceStateSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.zpi.slowomir.overlay.ui.voice.PickerOverlayManager

/**
 * Dedicated Foreground Service responsible for recording audio and running the Vosk
 * offline speech recognizer in compliance with Android 14+ background microphone requirements.
 */
@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    @Inject
    lateinit var stt: SpeechRecognizer

    @Inject
    lateinit var kws: KeywordSpotter

    @Inject
    lateinit var sink: VoiceInputSink

    @Inject
    lateinit var stateSource: VoiceStateSource

    @Inject
    lateinit var screenWaker: ScreenWaker

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var engineJob: Job? = null
    private var watchJob: Job? = null
    private var idleJob: Job? = null

    @Inject
    lateinit var mediaPlaybackMonitor: MediaPlaybackMonitor

    private var mediaJob: Job? = null

    // Needs no accessibility service: it only uses the context to reach the audio system services
    private val mediaExecutor by lazy { MediaExecutor(applicationContext) }

    @Volatile
    private var stopReason: String? = null

    private enum class Engine { KWS, STT, NONE }

    private fun VoiceState.engine() = when (this) {
        is VoiceState.KeywordSpotting -> Engine.KWS
        is VoiceState.Listening,
        is VoiceState.Dictating,
        is VoiceState.Editing,
        is VoiceState.TextProcessing,
        is VoiceState.IntentParsing,
        is VoiceState.CommandExecuting -> Engine.STT

        is VoiceState.Stopped, is VoiceState.Failed -> Engine.NONE
    }

    @Inject
    lateinit var pickerOverlayManager: PickerOverlayManager

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                Log.i(TAG, "Screen turned off, switching to keyword spotting")
                sink.accept(VoiceInput.EnableKwsMode)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stop requested")
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> Unit
            else -> return START_NOT_STICKY
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            fail("RECORD_AUDIO permission is not granted")
            return START_NOT_STICKY
        }

        if (!startForegroundInternal()) {
            fail("Failed to start foreground service")
            stopSelf()
            return START_NOT_STICKY
        }

        if (engineJob?.isActive == true) {
            Log.i(TAG, "Already running.")
            return START_NOT_STICKY
        }


        Log.i(TAG, "Starting in foreground")

        stopReason = null
        engineJob = scope.launch { superviseEngines() }
        idleJob = scope.launch { returnToKwsWhenIdle() }
        mediaJob = scope.launch { returnToKwsWhenMediaStarts() }
        watchJob = scope.launch {
            stateSource.state
                .dropWhile { it is VoiceState.Failed }
                .first { it is VoiceState.Failed }
            stopSelf()
        }


        sink.accept(VoiceInput.ServiceStarted)

        return START_NOT_STICKY
    }

    private suspend fun superviseEngines() {
        stateSource.state
            .map { it.engine() }
            .distinctUntilChanged()
            .collectLatest {
                when (it) {
                    Engine.KWS -> runKws()
                    Engine.STT -> runStt()
                    Engine.NONE -> Unit
                }
            }
    }

    /**
     * Goes back to keyword spotting when nothing is said for [LISTENING_TIMEOUT_MS]
     */
    private suspend fun returnToKwsWhenIdle() {
        stateSource.state.collectLatest { state ->
            if (state is VoiceState.Listening) {
                delay(LISTENING_TIMEOUT_MS)
                Log.i(TAG, "No speech for ${LISTENING_TIMEOUT_MS}ms, back to keyword spotting")
                sink.accept(VoiceInput.EnableKwsMode)
            }
        }
    }

    private suspend fun returnToKwsWhenMediaStarts() {
        mediaPlaybackMonitor.isPlaying.collect { playing ->
            if (playing && stateSource.state.value.engine() == Engine.STT) {
                Log.i(TAG, "Media started playing, switching to keyword spotting")
                sink.accept(VoiceInput.EnableKwsMode)
            }
        }
    }

    private suspend fun runKws() {
        kws.initialize().onFailure {
            sink.accept(VoiceInput.EnableSpeechRecognition)
            return
        }

        val cpuLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Slowomir:KeywordSpotting")
            .apply { setReferenceCounted(false) }
        cpuLock.acquire()

        try {
            kws.startListening().collect { result ->
                when (result) {
                    KwsResult.Detected -> {
                        if (!screenWaker.isScreenOn()) screenWaker.wakeUp()
                        if (mediaPlaybackMonitor.isPlayingNow()) {
                            Log.i(TAG, "Keyword detected while media is playing, pausing it")
                            mediaExecutor.pause()
                        }
                        sink.accept(VoiceInput.KeywordDetected)
                    }
                    is KwsResult.Error -> sink.accept(VoiceInput.EnableSpeechRecognition)
                }
            }
        } finally {
            kws.stopListening()
            if (cpuLock.isHeld) cpuLock.release()
        }
    }

    private suspend fun runStt() {
        stt.initialize().onFailure {
            fail(it.message ?: "Speech engine failed to initialize")
            return
        }
        try {
            stt.startListening()
                .catch { fail(it.message ?: "Speech engine failed") }
                .collect { result ->
                    when (result) {
                        is SttResult.Partial -> sink.accept(VoiceInput.SpeechPartial(result.text))
                        is SttResult.Final -> sink.accept(VoiceInput.SpeechFinal(result.text))
                        is SttResult.Error -> fail(result.exception.message ?: "Speech engine failed")
                    }
                }
        } finally {
            stt.stopListening()
        }
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

    private fun fail(reason: String) {
        Log.e(TAG, reason)
        stopReason = reason
        stopSelf()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOffReceiver)
        sink.accept(VoiceInput.ServiceStopped(stopReason))

        val job = engineJob
        scope.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            job?.join()
            kws.release()
            stt.release()
        }
        Log.i(TAG, "Destroyed")
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
        private const val LISTENING_TIMEOUT_MS = 30_000L

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
