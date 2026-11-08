package com.zpi.slowomir.voice.selection

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.utils.ApplicationUtils
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.VoiceIntent
import kotlin.collections.getOrNull

/**
 * A pending choice the user has to make before an intent can be turned into a command.
 */
sealed interface Selection {

    val pending: VoiceIntent

    val size: Int

    /**
     * Resolves the pending intent using the option at [index].
     *
     * @param index 1-based index of the chosen option.
     * @return The completed intent, or `null` if [index] is out of range.
     */
    fun resolve(index: Int): VoiceIntent?

    data class Contact(
        override val pending: VoiceIntent,
        val options: List<ContactOption>,
    ) : Selection {
        override val size: Int get() = options.size

        override fun resolve(index: Int): VoiceIntent? =
            options.getOrNull(index - 1)?.let {
                pending.copy(argument = CommandArgument.Text(it.number))
            }
    }

    data class Application(
        override val pending: VoiceIntent,
        val options: List<ApplicationUtils.AppItem>,
    ) : Selection {
        override val size: Int get() = options.size

        override fun resolve(index: Int): VoiceIntent? =
            options.getOrNull(index - 1)?.let {
                pending.copy(argument = CommandArgument.Text(it.packageName))
            }

    }
}

/** Outcome of [SelectionManager.begin]. */
sealed interface SelectionResult {
    data class Resolved(val intent: VoiceIntent) : SelectionResult
    data object Pending : SelectionResult
    data class Failed(val reason: String) : SelectionResult
}

/** Outcome of [SelectionManager.handleSpeech]. */
sealed interface SelectionSpeechResult {
    data class Chosen(val intent: VoiceIntent) : SelectionSpeechResult
    data class Cancelled(val rawText: String) : SelectionSpeechResult
    data class Ignored(val rawText: String) : SelectionSpeechResult
}
