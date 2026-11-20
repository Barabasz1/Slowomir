package com.zpi.slowomir.voice.selection

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.macro.MacroInfo
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

    data class Macro(
        override val pending: VoiceIntent,
        val options: List<MacroInfo>,
    ) : Selection {
        override val size: Int get() = options.size

        override fun resolve(index: Int): VoiceIntent? =
            if (index in 1..options.size) {
                pending.copy(argument = CommandArgument.Number(index))
            } else null
    }
}

/** Outcome of [SelectionManager.begin]. */
sealed interface SelectionResult {
    data class Resolved(val intent: VoiceIntent) : SelectionResult
    data object Pending : SelectionResult
    data class Failed(val failure: SelectionFailure) : SelectionResult
}

/** Why [SelectionManager.begin] could not start or resolve a selection. */
sealed interface SelectionFailure {
    data class Unsupported(val identifier: CommandIdentifier) : SelectionFailure
    data class ContactNotFound(val query: String) : SelectionFailure
    data object ContactsPermissionDenied : SelectionFailure
    data object NoContactGiven : SelectionFailure
    data class ApplicationNotFound(val query: String) : SelectionFailure
    data object NoApplicationGiven : SelectionFailure
    data object NoMacros : SelectionFailure
}

/** Outcome of [SelectionManager.handleSpeech]. */
sealed interface SelectionSpeechResult {
    data class Chosen(val intent: VoiceIntent) : SelectionSpeechResult
    data class Cancelled(val rawText: String) : SelectionSpeechResult
    data class Ignored(val rawText: String) : SelectionSpeechResult
}
