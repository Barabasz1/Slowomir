package com.zpi.slowomir.voice.selection

import android.content.Context
import android.util.Log
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.utils.ApplicationUtils
import com.zpi.slowomir.utils.ApplicationUtils.ApplicationLookupResult
import com.zpi.slowomir.utils.ContactLookupResult
import com.zpi.slowomir.utils.ContactUtils
import com.zpi.slowomir.voice.PolishTextNormalizer
import com.zpi.slowomir.voice.VoiceIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles resolving ambiguous intent arguments by interpreting user choice and returns complete intent.
 */
@Singleton
class SelectionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val contactUtils: ContactUtils,
    private val applicationUtils: ApplicationUtils,
    private val normalizer: PolishTextNormalizer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var timeoutJob: Job? = null

    private val _selection = MutableStateFlow<Selection?>(null)

    val selection: StateFlow<Selection?> = _selection.asStateFlow()

    val isActive: Boolean get() = _selection.value != null

    /**
     * Resolves the ambiguous argument of [intent].
     * If several options match, a [Selection] is published and [SelectionResult.Pending] is returned.
     */
    suspend fun begin(intent: VoiceIntent): SelectionResult = when (intent.identifier) {
        CommandIdentifier.PHONE_CALL,
        CommandIdentifier.SMS_WRITE -> beginContact(intent)
        CommandIdentifier.NAVIGATE_APP_OPEN -> beginApplication(intent)

        else -> {
            Log.w(TAG, "No selection defined for ${intent.identifier}")
            SelectionResult.Failed("Selection is not supported for ${intent.identifier}")
        }
    }

    /**
     * Interprets speech while a selection is pending.
     */
    fun handleSpeech(text: String): SelectionSpeechResult {
        val normalized = normalizer.normalize(text)

        if (normalized in cancelPhrases) {
            cancel()
            return SelectionSpeechResult.Cancelled(text)
        }

        val index = selectionRegex.matchEntire(normalized)?.groupValues?.get(1)?.toIntOrNull()
        if (index == null) {
            Log.d(TAG, "Ignoring speech during selection: '$text'")
            return SelectionSpeechResult.Ignored(text)
        }

        val intent = choose(index)
        if (intent == null) {
            Log.d(TAG, "Selection $index ignored (no pending selection or out of range)")
            return SelectionSpeechResult.Ignored(text)
        }
        return SelectionSpeechResult.Chosen(intent)
    }

    /**
     * Picks the option at [index] and clears the selection.
     *
     * @param index 1-based index of the option.
     * @return The completed intent, or `null` if there is no selection or [index] is out of range.
     */
    fun choose(index: Int): VoiceIntent? {
        val current = _selection.value ?: return null
        val intent = current.resolve(index) ?: return null
        val won = _selection.compareAndSet(current, null)
        if (won) {
            timeoutJob?.cancel()
            Log.d(TAG, "Selection $index chosen -> ${intent.identifier} ${intent.argument}")
        }
        return if (won) intent else null
    }

    /**
     * Clears the pending selection.
     *
     * @return `true` if there was a selection to cancel.
     */
    fun cancel(): Boolean {
        val cancelled = _selection.getAndUpdate { null } != null
        if (cancelled) {
            timeoutJob?.cancel()
            Log.d(TAG, "Selection cancelled")
        }
        return cancelled
    }

    /**
     * Publishes [selection] and starts its timeout.
     */
    private fun publish(selection: Selection) {
        timeoutJob?.cancel()
        _selection.value = selection
        timeoutJob = scope.launch {
            delay(DEFAULT_TIMEOUT_MS)
            val current = _selection.value
            if (current === selection && _selection.compareAndSet(current, null)) {
                Log.i(TAG, "Selection timed out")
            }
        }
    }

    private suspend fun beginContact(intent: VoiceIntent): SelectionResult {
        val query = (intent.argument as? CommandArgument.Text)?.value.orEmpty()

        return when (val result =
            withContext(Dispatchers.IO) { contactUtils.lookup(appContext, query) }) {
            is ContactLookupResult.Found ->
                SelectionResult.Resolved(intent.copy(argument = CommandArgument.Text(result.number)))

            is ContactLookupResult.Ambiguous -> {
                publish(Selection.Contact(intent, result.options))
                SelectionResult.Pending
            }

            ContactLookupResult.NotFound -> SelectionResult.Failed("Could not find a contact: $query")
            ContactLookupResult.PermissionDenied -> SelectionResult.Failed("No access to reading contacts")
            ContactLookupResult.EmptyQuery -> SelectionResult.Failed("No contact was given.")
        }
    }

    private suspend fun beginApplication(intent: VoiceIntent): SelectionResult {
        val query = (intent.argument as? CommandArgument.Text)?.value.orEmpty()

        return when (val result =
            withContext(Dispatchers.IO) { applicationUtils.lookup(query) }) {
            is ApplicationLookupResult.Found ->
                SelectionResult.Resolved(intent.copy(argument = CommandArgument.Text(result.packageName)))

            is ApplicationLookupResult.Ambiguous -> {
                publish(Selection.Application(intent, result.options))
                SelectionResult.Pending
            }

            ApplicationLookupResult.NotFound -> SelectionResult.Failed("Could not find a contact: $query")
            ApplicationLookupResult.EmptyQuery -> SelectionResult.Failed("No contact was given.")
        }
    }

    companion object {
        private const val TAG = "SelectionManager"
        private const val DEFAULT_TIMEOUT_MS = 15_000L

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
