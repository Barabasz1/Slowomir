package com.zpi.slowomir.overlay.ui.confirmation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject
import javax.inject.Singleton


class ConfirmationRequest(
    val title: String,
    val onYes: () -> Unit,
    val onNo: () -> Unit,
    val yesLabel: String = "Tak",
    val noLabel: String = "Nie",
    val content: @Composable () -> Unit,
)

enum class ConfirmationSpeechResult { Yes, No, Ignored }

@Singleton
class ConfirmationManager @Inject constructor() {
    private val _request = MutableStateFlow<ConfirmationRequest?>(null)
    val request: StateFlow<ConfirmationRequest?> = _request.asStateFlow()

    val isActive: Boolean get() = _request.value != null

    fun ask(request: ConfirmationRequest) {
        _request.value = request
    }

    fun dismiss(request: ConfirmationRequest? = null) {
        if (request == null || _request.value === request) _request.value = null
    }


    fun answer(yes: Boolean): Boolean {
        val answered = _request.getAndUpdate { null } ?: return false
        if (yes) answered.onYes() else answered.onNo()
        return true
    }

    fun handleSpeech(normalized: String): ConfirmationSpeechResult {
        val result = when (normalized.trim()) {
            in YES_PHRASES -> ConfirmationSpeechResult.Yes
            in NO_PHRASES -> ConfirmationSpeechResult.No
            else -> return ConfirmationSpeechResult.Ignored
        }
        answer(result == ConfirmationSpeechResult.Yes)
        return result
    }

    private companion object {
        val YES_PHRASES = setOf("tak", "zgoda", "potwierdzam")
        val NO_PHRASES = setOf("nie", "odrzuć", "anuluj")
    }
}
