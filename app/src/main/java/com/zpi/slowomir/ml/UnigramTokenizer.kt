package com.zpi.slowomir.ml

import android.content.Context
import org.json.JSONObject

/**
 * [TextTokenizer] for a Hugging Face `tokenizer.json` asset with a Unigram model, no normalizer,
 * WhitespaceSplit pre-tokenizing and `<s> A </s>` post-processing.
 */
class UnigramTokenizer(
    context: Context,
    private val assetPath: String,
    private val prependSpace: Boolean = false,
) : TextTokenizer {

    private val appContext = context.applicationContext
    private val lock = Any()

    @Volatile
    private var model: UnigramModel? = null

    override fun load() {
        get()
    }

    override fun encode(text: String, maxLength: Int): TokenEncoding = get().encode(text, maxLength)

    override fun close() {
        synchronized(lock) { model = null }
    }

    private fun get(): UnigramModel = model ?: synchronized(lock) {
        model ?: run {
            val json =
                appContext.assets.open(assetPath).use { it.readBytes().toString(Charsets.UTF_8) }
            UnigramModel.fromTokenizerJson(json, prependSpace)
        }.also { model = it }
    }
}

class UnigramModel(
    pieces: List<String>,
    private val scores: FloatArray,
    private val unkId: Int,
    private val bosId: Int,
    private val eosId: Int,
    private val prependSpace: Boolean = false,
) {
    private val pieceToId: Map<String, Int> = HashMap<String, Int>(pieces.size * 2).also { map ->
        pieces.forEachIndexed { i, p -> map.putIfAbsent(p, i) }
    }
    private val maxPieceLength: Int = pieces.maxOf { it.length }

    private val unkScore: Float =
        (scores.filterIndexed { i, _ -> i != unkId }.minOrNull() ?: 0f) - UNK_PENALTY

    fun encode(text: String, maxLength: Int): TokenEncoding {
        val ids = ArrayList<Long>()
        val offsets = ArrayList<IntRange?>()

        ids += bosId.toLong(); offsets += null

        val budget = maxLength - 2
        var i = 0
        outer@ while (i < text.length) {
            if (text[i].isWhitespace()) {
                i++; continue
            }
            val wordStart = i
            while (i < text.length && !text[i].isWhitespace()) i++

            for ((id, range) in segment(text.substring(wordStart, i), wordStart)) {
                if (ids.size - 1 >= budget) break@outer
                ids += id.toLong(); offsets += range
            }
        }

        ids += eosId.toLong(); offsets += null
        return TokenEncoding(ids.toLongArray(), offsets)
    }

    private fun segment(rawWord: String, baseOffset: Int): List<Pair<Int, IntRange>> {
        val word = if (prependSpace) "▁$rawWord" else rawWord
        val shift = if (prependSpace) 1 else 0
        val n = word.length
        val best = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val prevStart = IntArray(n + 1) { -1 }
        val prevId = IntArray(n + 1) { unkId }
        best[0] = 0f

        for (start in 0 until n) {
            if (best[start] == Float.NEGATIVE_INFINITY) continue
            var hasSingle = false
            val maxLen = minOf(maxPieceLength, n - start)
            for (len in 1..maxLen) {
                val id = pieceToId[word.substring(start, start + len)] ?: continue
                if (len == 1) hasSingle = true
                val cand = best[start] + scores[id]
                if (cand > best[start + len]) {
                    best[start + len] = cand
                    prevStart[start + len] = start
                    prevId[start + len] = id
                }
            }
            if (!hasSingle) {
                val cand = best[start] + unkScore
                if (cand > best[start + 1]) {
                    best[start + 1] = cand
                    prevStart[start + 1] = start
                    prevId[start + 1] = unkId
                }
            }
        }

        val result = ArrayList<Pair<Int, IntRange>>()
        var end = n
        while (end > 0) {
            val start = prevStart[end]
            val from = maxOf(start - shift, 0)
            result += prevId[end] to (baseOffset + from until baseOffset + end - shift)
            end = start
        }
        result.reverse()

        val fused = ArrayList<Pair<Int, IntRange>>()
        for (p in result) {
            val last = fused.lastOrNull()
            if (p.first == unkId && last != null && last.first == unkId) {
                fused[fused.lastIndex] = unkId to (last.second.first..p.second.last)
            } else {
                fused += p
            }
        }
        return fused
    }

    companion object {
        private const val UNK_PENALTY = 10f

        fun fromTokenizerJson(json: String, prependSpace: Boolean = false): UnigramModel {
            val model = JSONObject(json).getJSONObject("model")
            check(model.getString("type") == "Unigram") { "Expected a Unigram tokenizer" }

            val vocab = model.getJSONArray("vocab")
            val pieces = ArrayList<String>(vocab.length())
            val scores = FloatArray(vocab.length())
            for (i in 0 until vocab.length()) {
                val entry = vocab.getJSONArray(i)
                pieces += entry.getString(0)
                scores[i] = entry.getDouble(1).toFloat()
            }

            val lookup = pieces.withIndex().associate { it.value to it.index }
            return UnigramModel(
                pieces = pieces,
                scores = scores,
                unkId = model.getInt("unk_id"),
                bosId = checkNotNull(lookup["<s>"]) { "<s> missing from vocab" },
                eosId = checkNotNull(lookup["</s>"]) { "</s> missing from vocab" },
                prependSpace = prependSpace,
            )
        }
    }
}
