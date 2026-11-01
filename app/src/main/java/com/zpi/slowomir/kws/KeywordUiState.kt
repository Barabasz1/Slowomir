package com.zpi.slowomir.kws

data class KeywordUiState(
    val detections: List<String> = emptyList(),
    val isListening: Boolean = false,
    val error: KeywordError? = null
)