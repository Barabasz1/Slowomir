package com.zpi.slowomir.voice.dictation

/**
 * Restores punctuation and capitalization.
 */
interface PunctuationRestorer {
    suspend fun initialize(): Result<Unit>

    /**
     * Returns [text] with punctuation and casing restored, or [text] unchanged if the model is unavailable.
     *
     * @param punctuation When false the punctuation the model predicts is dropped the casing of the words is kept.
     */
    suspend fun restore(text: String, punctuation: Boolean = true): String

    fun release()
}

