package com.zpi.slowomir.kws

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.sqrt


class KeywordSpotterManager @Inject constructor(
    @ApplicationContext context: Context,
) : KeywordSpotter {

    private val appContext = context.applicationContext
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()

    @Volatile
    private var session: OrtSession? = null

    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var inputName: String? = null

    private val lifecycleLock = Any()

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var frontend: MicroFrontend? = null

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(lifecycleLock) {
            runCatching {
                if (session == null) session = loadSession()
                if (frontend == null) {
                    frontend = MicroFrontend().also {
                        check(it.init()) {
                            "Frontend init failed"
                        }
                    }
                }
            }.onFailure {
                Log.e(TAG, "KWS init failed", it)
            }
        }
    }

    override fun startListening(): Flow<KwsResult> = callbackFlow {
        val thread: Thread

        synchronized(lifecycleLock) {
            val sess = session
            val fe = frontend

            if (sess == null || fe == null) {
                trySend(KwsResult.Error(KeywordError.MODEL_LOAD_FAILED))
                close()
                return@callbackFlow
            }

            if (worker?.isAlive == true) {
                trySend(KwsResult.Error(KeywordError.UNKNOWN))
                close()
                return@callbackFlow
            }

            running = true

            thread = Thread({
                try {
                    runLoop(sess, fe) { result ->
                        trySend(result)
                    }
                } finally {
                    synchronized(lifecycleLock) {
                        running = false
                        if (worker === Thread.currentThread()) {
                            worker = null
                        }
                    }
                    close()
                }
            }, "kws-audio")

            worker = thread
            thread.start()
        }

        awaitClose {
            stopListening()

            if (thread !== Thread.currentThread()) {
                thread.join()
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun stopListening() {
        synchronized(lifecycleLock) {
            running = false
            runCatching {
                audioRecord?.stop()
            }
        }

    }

    override fun release() {
        val thread = synchronized(lifecycleLock) {
            running = false
            runCatching { audioRecord?.stop() }
            worker
        }

        if (thread != null && thread !== Thread.currentThread()) {
            thread.join()
        }

        synchronized(lifecycleLock) {
            worker = null

            session?.close()
            session = null

            frontend?.close()
            frontend = null

            inputName = null
        }
    }


    private fun loadSession(): OrtSession {
        val bytes = appContext.assets.open(MODEL_FILE).use { it.readBytes() }
        val created = OrtSession.SessionOptions().use { opts ->
            opts.setIntraOpNumThreads(2)
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            ortEnv.createSession(bytes, opts)
        }
        inputName = created.inputNames.first()
        return created
    }

    @SuppressLint("MissingPermission")
    private fun runLoop(sess: OrtSession, fe: MicroFrontend, emit: (KwsResult) -> Unit) {
        var record: AudioRecord? = null
        try {
            val inName = inputName ?: sess.inputNames.first()

            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) {
                emit(KwsResult.Error(KeywordError.MIC_UNAVAILABLE))
                return
            }
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf * 2, SAMPLE_RATE * 2)
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                emit(KwsResult.Error(KeywordError.MIC_UNAVAILABLE))
                return
            }

            val shouldStart = synchronized(lifecycleLock) {
                if (!running) {
                    false
                } else {
                    audioRecord = record
                    record.startRecording()
                    true
                }
            }

            if (!shouldStart) return

            val window = ShortArray(WINDOW_SAMPLES)
            val chunk = ShortArray(CHUNK_SAMPLES)
            val inputBuf = ByteBuffer.allocateDirect(FRAMES * BINS * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            val recent = ArrayDeque<Float>()
            var sinceInfer = 0
            var cooldownChunks = 0

            while (running) {
                val n = record.read(chunk, 0, chunk.size)
                if (n < 0) {
                    Log.w(TAG, "AudioRecord.read error: $n")
                    if (running) {
                        emit(KwsResult.Error(KeywordError.MIC_UNAVAILABLE))
                    }
                    break
                }
                if (n == 0) continue

                System.arraycopy(window, n, window, 0, WINDOW_SAMPLES - n)
                System.arraycopy(chunk, 0, window, WINDOW_SAMPLES - n, n)

                if (cooldownChunks > 0) {
                    cooldownChunks--; continue
                }
                if (++sinceInfer < INFER_EVERY_CHUNKS) continue
                sinceInfer = 0

                if (rms(window) < MIN_RMS) {
                    recent.clear(); continue
                }

                val features = fe.process(window)
                inputBuf.rewind()
                inputBuf.put(features)
                inputBuf.rewind()

                val pTarget = OnnxTensor.createTensor(ortEnv, inputBuf, INPUT_SHAPE).use { tensor ->
                    sess.run(mapOf(inName to tensor)).use { result ->
                        @Suppress("UNCHECKED_CAST")
                        val probs = (result[0].value as Array<FloatArray>)[0]
                        probs[TARGET_INDEX]
                    }
                }

                recent.addLast(pTarget)
                if (recent.size > SMOOTH) recent.removeFirst()

                if (recent.size == SMOOTH && recent.average() >= THRESHOLD) {
                    Log.d(TAG, "KWS trigger: p=${recent.average()}")
                    recent.clear()
                    window.fill(0)
                    cooldownChunks = COOLDOWN_CHUNKS
                    emit(KwsResult.Detected)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Błąd w pętli KWS", t)
            emit(KwsResult.Error(KeywordError.UNKNOWN))
        } finally {
            synchronized(lifecycleLock) {
                if (audioRecord === record) {
                    audioRecord = null
                }
            }

            runCatching { record?.stop() }
            runCatching { record?.release() }
        }
    }

    private fun rms(buf: ShortArray): Double {
        var sum = 0.0
        for (s in buf) sum += s.toDouble() * s
        return sqrt(sum / buf.size)
    }

    private companion object {
        const val TAG = "KeywordSpotterManager"
        const val MODEL_FILE = "slowomir_kws.onnx"

        /*
        SOURCE ATTRIBUTION:
        Base pretrained model: multilingual_context_73_0.8011
        Original project: Harvard Edge – multilingual_kws
        Repository: https://github.com/harvard-edge/multilingual_kws
        */
        const val SAMPLE_RATE = 16000
        const val WINDOW_SAMPLES = 16000
        const val CHUNK_SAMPLES = 1600
        const val FRAMES = 49
        const val BINS = 40
        const val NUM_CLASSES = 3
        const val TARGET_INDEX = 2
        val INPUT_SHAPE = longArrayOf(1, FRAMES.toLong(), BINS.toLong(), 1)

        const val INFER_EVERY_CHUNKS = 2
        const val SMOOTH = 2
        const val THRESHOLD = 0.8f
        const val COOLDOWN_CHUNKS = 12
        const val MIN_RMS = 150.0
    }
}