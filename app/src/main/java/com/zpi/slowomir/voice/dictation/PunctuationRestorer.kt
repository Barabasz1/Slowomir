package com.zpi.slowomir.voice.dictation

/** Restores punctuation and capitalization. */
interface PunctuationRestorer {
    suspend fun initialize(): Result<Unit>

    /** Returns [text] with punctuation and casing restored, or [text] unchanged if the model is unavailable. */
    suspend fun restore(text: String): String

    fun release()
}

