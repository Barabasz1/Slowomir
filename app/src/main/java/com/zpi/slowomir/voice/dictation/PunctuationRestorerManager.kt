package com.zpi.slowomir.voice.dictation

import android.content.Context
import android.util.Log
import com.zpi.slowomir.ml.OnnxTextModel
import com.zpi.slowomir.ml.UnigramTokenizer
import com.zpi.slowomir.ml.argmax
import com.zpi.slowomir.ml.isWordStart
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PunctuationRestorerManager @Inject constructor(
    @ApplicationContext context: Context,
) : PunctuationRestorer {

    private val model = OnnxTextModel(
        context = context,
        modelAsset = "$ASSET_DIR/model.int8.onnx",
        tokenizer = UnigramTokenizer(context, "$ASSET_DIR/tokenizer.json", prependSpace = true),
        maxLength = MAX_TOKENS,
    )

    override suspend fun initialize(): Result<Unit> = model.initialize()

    override suspend fun restore(text: String, punctuation: Boolean): String {
        val words = text.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return ""
        model.initialize().onFailure {
            Log.e(TAG, "Punctuation model unavailable", it)
            return text.trim()
        }

        return try {
            val parts = ArrayList<String>()
            for (chunk in words.chunked(CHUNK_WORDS)) parts += restoreChunk(chunk, punctuation)
            parts.joinToString(" ")
        } catch (t: Throwable) {
            Log.e(TAG, "Punctuation restoration failed", t)
            text.trim()
        }
    }

    override fun release() = model.release()

    private suspend fun restoreChunk(words: List<String>, punctuation: Boolean): String {
        val lower = words.map { it.lowercase() }
        val joined = FILLER + " " + lower.joinToString(" ")
        val total = words.size + 1
        val output = model.infer(joined) ?: return words.joinToString(" ")

        val punctLogits = output.tokenOutputs.firstOrNull { it.firstOrNull()?.size == PUNCT.size }
        val caseLogits = output.tokenOutputs.firstOrNull { it.firstOrNull()?.size == CASE_CLASSES }
        if (punctLogits == null || caseLogits == null) return words.joinToString(" ")

        val punct = IntArray(total)
        val case = IntArray(total)
        var word = -1
        var lastStart = -1
        for (i in output.encoding.offsets.indices) {
            val range = output.encoding.offsets[i] ?: continue
            if (!isWordStart(joined, range) || range.first == lastStart) continue
            lastStart = range.first
            word++
            if (word >= total) break
            punct[word] = argmax(punctLogits[i])
            case[word] = argmax(caseLogits[i])
        }

        Log.d(TAG, "[RESTORE] input='$joined' labels(case/punct, first is the filler)=" +
            (0 until total).joinToString(" ") { "${if (it == 0) FILLER else lower[it - 1]}:${case[it]}/${punct[it]}" })

        val labelled = ((word + 1).coerceAtMost(total) - 1).coerceAtLeast(0)

        return buildString {
            for (w in 0 until words.size) {
                if (w > 0) append(' ')
                val token = lower[w]
                if (w < labelled) {
                    when (case[w + 1]) {
                        CASE_UPPER -> append(token.uppercase())
                        CASE_TITLE -> append(token.replaceFirstChar { it.uppercase() })
                        else -> append(token)
                    }
                    if (punctuation) append(PUNCT[punct[w + 1]])
                } else {
                    append(token)
                }
            }
        }
    }

    private companion object {
        const val TAG = "PunctuationRestorer"
        const val ASSET_DIR = "sdadas-polish-distilroberta-punct"
        const val MAX_TOKENS = 160
        const val CHUNK_WORDS = 40
        const val FILLER = "i"
        const val CASE_CLASSES = 3
        const val CASE_TITLE = 1
        const val CASE_UPPER = 2
        val WHITESPACE = Regex("\\s+")

        /** Same order as in training (Do not change) */
        val PUNCT = listOf("", ",", ".", "?", "!", ":", ";", "...")
    }
}