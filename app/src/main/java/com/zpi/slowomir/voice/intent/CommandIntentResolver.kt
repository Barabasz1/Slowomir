package com.zpi.slowomir.voice.intent

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier

enum class IntentResolverError {
    MODEL_LOAD_FAILED,
    NOT_INITIALIZED,
    INFERENCE_FAILED
}

sealed interface IntentResolution {
    data class Resolved(
        val identifier: CommandIdentifier,
        val confidence: Float,
        val argument: CommandArgument? = null,
    ) : IntentResolution

    data object Unrecognized : IntentResolution

    data class Error(val error: IntentResolverError) : IntentResolution
}

/**
 * Classifies a normalized utterance into a [CommandIdentifier] and extracts its argument
 * using the on-device encoder model.
 */
interface CommandIntentResolver {
    suspend fun initialize(): Result<Unit>

    suspend fun resolve(text: String): IntentResolution

    fun release()
}
