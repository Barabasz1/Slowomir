package com.zpi.slowomir.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ModelOutput(
    val encoding: TokenEncoding,
    val sequenceLogits: FloatArray?,
    val tokenOutputs: List<Array<FloatArray>> = emptyList(),
) {
    val tokenLogits: Array<FloatArray>? get() = tokenOutputs.lastOrNull()
}

/**
 * Generic wrapper on ONNX text encoder model.
 **/
class OnnxTextModel(
    context: Context,
    private val modelAsset: String,
    private val tokenizer: TextTokenizer,
    private val maxLength: Int,
    private val intraOpThreads: Int = 2,
) {
    private val appContext = context.applicationContext
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val lifecycleLock = Any()
    private val runLock = ReentrantReadWriteLock()

    @Volatile
    private var session: OrtSession? = null

    @Volatile
    private var inputNames: Set<String> = emptySet()

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        synchronized(lifecycleLock) {
            runCatching {
                tokenizer.load()
                if (session == null) session = loadSession()
            }.onFailure {
                Log.e(TAG, "Model init failed: $modelAsset", it)
            }
        }
    }

    suspend fun infer(text: String): ModelOutput? = withContext(Dispatchers.Default) {
        runLock.read { inferLocked(text) }
    }

    private fun inferLocked(text: String): ModelOutput? {
        val sess = session ?: return null

        val enc = tokenizer.encode(text, maxLength)
        val shape = longArrayOf(1, enc.ids.size.toLong())
        val mask = LongArray(enc.ids.size) { 1L }

        var sequence: FloatArray? = null
        val allTokens = ArrayList<Array<FloatArray>>()

        OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(enc.ids), shape).use { idsTensor ->
            OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(mask), shape).use { maskTensor ->
                val feed = buildMap<String, OnnxTensor> {
                    put(INPUT_IDS, idsTensor)
                    if (ATTENTION_MASK in inputNames) put(ATTENTION_MASK, maskTensor)
                }
                sess.run(feed).use { result ->
                    for ((_, out) in result) {
                        val batch = out.value as? Array<*> ?: continue
                        when (val first = batch.firstOrNull()) {
                            is FloatArray -> sequence = first
                            is Array<*> -> {
                                @Suppress("UNCHECKED_CAST")
                                allTokens += first as Array<FloatArray>
                            }
                        }
                    }
                }
            }
        }
        return ModelOutput(enc, sequence, allTokens)
    }

    fun release() {
        runLock.write {
            synchronized(lifecycleLock) {
                session?.close()
                session = null
                inputNames = emptySet()
                tokenizer.close()
            }
        }
    }

    private fun loadSession(): OrtSession {
        val bytes = appContext.assets.open(modelAsset).use { it.readBytes() }
        val created = OrtSession.SessionOptions().use { opts ->
            opts.setIntraOpNumThreads(intraOpThreads)
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            ortEnv.createSession(bytes, opts)
        }
        inputNames = created.inputNames
        return created
    }

    private companion object {
        const val TAG = "OnnxTextModel"
        const val INPUT_IDS = "input_ids"
        const val ATTENTION_MASK = "attention_mask"
    }
}
