package com.zpi.slowomir.kws

import kotlinx.coroutines.flow.Flow

sealed interface KwsResult {
    data object Detected : KwsResult
    data class Error(val error: KeywordError) : KwsResult
}


interface KeywordSpotter {
    suspend fun initialize(): Result<Unit>

    fun startListening(): Flow<KwsResult>
    fun stopListening()
    fun release()
}