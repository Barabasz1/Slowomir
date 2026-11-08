package com.zpi.slowomir.voice

import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of handling speech input during an active contact selection dialog.
 */
sealed interface ContactSelectionSpeechResult {
    data class Selected(val intent: VoiceIntent, val command: Command) : ContactSelectionSpeechResult
    data class Cancelled(val rawText: String) : ContactSelectionSpeechResult
    data class Unrecognized(val rawText: String) : ContactSelectionSpeechResult
}

/**
 * Manages the contact selection state, parsing spoken choices or cancel requests,
 * and dispatching the resolved call or SMS commands.
 */
@Singleton
class ContactSelectionManager @Inject constructor(
    private val mediator: CommandMediator,
    private val commandResolver: CommandResolver,
    private val normalizer: PolishTextNormalizer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val selectionMutex = Mutex()

    /**
     * Handles speech when a contact selection is pending.
     *
     * @param text The recognized speech text.
     * @return The [ContactSelectionSpeechResult] indicating whether a contact was selected, canceled or not recognized.
     */
    suspend fun handleSpeech(text: String): ContactSelectionSpeechResult {
        if (isCancel(text)) {
            cancelSelectionInternal()
            return ContactSelectionSpeechResult.Cancelled(text)
        }

        val index = parseSelection(text)
        if (index == null) {
            Log.d(TAG, "Ignoring speech during contact selection: '$text'")
            return ContactSelectionSpeechResult.Unrecognized(text)
        }

        val (option, action) = consumeSelection(index) ?: run {
            Log.d(TAG, "Selection $index ignored (no pending selection or out of range)")
            return ContactSelectionSpeechResult.Unrecognized(text)
        }

        val recipient = option.number
        val command = when (action) {
            CommandIdentifier.SMS_WRITE -> commandResolver.createWriteSMS(recipient)
            else -> commandResolver.createCall(recipient)
        }

        val intent = VoiceIntent(
            identifier = action ?: CommandIdentifier.PHONE_CALL,
            argument = CommandArgument.Text(recipient),
            rawText = text,
        )

        return ContactSelectionSpeechResult.Selected(intent, command)
    }

    /**
     * Selects a contact by its 1-based index (e.g. from a UI click).
     *
     * @param index The 1-based index of the chosen contact option.
     */
    fun selectContact(index: Int) {
        scope.launch {
            val (option, action) = consumeSelection(index) ?: run {
                Log.d(TAG, "Selection $index ignored (no pending selection or out of range)")
                return@launch
            }

            val recipient = option.number
            val command = when (action) {
                CommandIdentifier.SMS_WRITE -> commandResolver.createWriteSMS(recipient)
                else -> commandResolver.createCall(recipient)
            }

            mediator.handleEvent(OverlayEvent.CommandInterpreted(command))
        }
    }

    /**
     * Cancels the current contact selection.
     */
    fun cancelSelection() {
        scope.launch { cancelSelectionInternal() }
    }

    private suspend fun cancelSelectionInternal() {
        selectionMutex.withLock {
            mediator.handleEvent(OverlayEvent.SelectionCleared)
        }
    }

    private suspend fun consumeSelection(index: Int): Pair<ContactOption, CommandIdentifier?>? = selectionMutex.withLock {
        val options = mediator.mediatorState.value.contactSelection ?: return@withLock null
        val option = options.getOrNull(index - 1) ?: return@withLock null
        val action = mediator.mediatorState.value.contactSelectionAction
        mediator.handleEvent(OverlayEvent.SelectionCleared)
        Pair(option, action)
    }

    private fun isCancel(text: String): Boolean = normalizer.normalize(text) in cancelPhrases

    private fun parseSelection(text: String): Int? {
        val normalized = normalizer.normalize(text)
        return selectionRegex.matchEntire(normalized)?.groupValues?.get(1)?.toIntOrNull()
    }

    companion object {
        private const val TAG = "ContactSelectionManager"

        private val selectionRegex =
            Regex("""^(?:(?:numer|opcja|opcję|pozycja|pozycję|kontakt|wybierz|wybieram)\s+)*(\d+)$""")

        private val cancelPhrases = setOf(
            "anuluj",
            "zamknij",
            "nie",
            "wróć",
            "cofnij",
            "stop",
            "wyjdź",
        )
    }
}
