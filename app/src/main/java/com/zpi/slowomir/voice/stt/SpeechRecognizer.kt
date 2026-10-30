package com.zpi.slowomir.voice.stt

import kotlinx.coroutines.flow.Flow

sealed interface SttResult {
    data class Partial(val text: String) : SttResult
    data class Final(val text: String) : SttResult
    data class Error(val exception: Exception) : SttResult
}

interface SpeechRecognizer {
    suspend fun initialize(): Result<Unit>
    fun startListening(): Flow<SttResult>
}