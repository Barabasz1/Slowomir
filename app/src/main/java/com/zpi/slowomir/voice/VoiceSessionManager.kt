package com.zpi.slowomir.voice

import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.stt.InternalEvent
import com.zpi.slowomir.voice.stt.SessionEvent
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceInputSink
import com.zpi.slowomir.voice.stt.VoiceState
import com.zpi.slowomir.voice.stt.VoiceStateMachine
import com.zpi.slowomir.voice.stt.VoiceStateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice session manager. Manages the state of the voice session and dispatches commands.
 */
@Singleton
class VoiceSessionManager @Inject constructor(
    private val mediator: CommandMediator,
    private val intentParser: CommandIntentParser,
    private val normalizer: PolishTextNormalizer,
    private val commandResolver: CommandResolver,
) : VoiceInputSink, VoiceStateSource {

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Stopped)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val inbox = Channel<SessionEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val selectionMutex = Mutex()

    init {
        scope.launch {
            for (input in inbox) handle(input)
        }
    }

    private suspend fun handle(input: SessionEvent) {
        when (input) {
            is VoiceInput.SpeechPartial -> {
                mediator.handleEvent(OverlayEvent.SpeechPartial(input.text))
            }

            is VoiceInput.KeywordDetected -> {
                mediator.handleEvent(OverlayEvent.WakeWordDetected)
            }

            else -> {}
        }

        val oldState = _state.value
        val newState = VoiceStateMachine.reduce(oldState, input)
        _state.value = newState

        if (oldState != newState) {
            Log.d(TAG, "State transition: $oldState + $input -> $newState")
            onStateChanged(newState)
        }
    }

    /**
     * Handles a state transition.
     *
     * @param newState The new state.
     */
    private fun onStateChanged(newState: VoiceState) {
        when (newState) {
            is VoiceState.IntentParsing -> {
                parseIntent(newState.rawText)
            }

            is VoiceState.CommandExecuting -> {
                dispatchCommand(newState.intent, newState.command)
            }

            else -> {}
        }
    }

    /**
     * Parses a command intent.
     *
     * @param text The text to parse.
     */
    private fun parseIntent(text: String) {
        if (text.isBlank()) {
            return
        }

        scope.launch {
            mediator.handleEvent(OverlayEvent.ProcessingCommand(text))

            if (mediator.mediatorState.value.contactSelection != null) {
                handleSelectionSpeech(text)
                return@launch
            }

            val intent = intentParser.parse(text)

            if (intent == null) {
                mediator.handleEvent(OverlayEvent.InvalidCommand(text))
                inbox.send(InternalEvent.IntentFailed(text))
                return@launch
            }

            val command = commandResolver.resolve(intent)
            inbox.send(InternalEvent.IntentParsed(intent, command))
        }
    }

    /**
     * Executes a command.
     *
     * @param intent The command intent.
     * @param command The command to execute.
     */
    private fun dispatchCommand(intent: VoiceIntent, command: Command) {
        scope.launch {
            mediator.handleEvent(OverlayEvent.CommandInterpreted(command))
            val returnToKws = intent.identifier == CommandIdentifier.OVERLAY_STOP

            inbox.send(InternalEvent.CommandFinished(CommandResult.Success(), returnToKws = returnToKws))
        }
    }

    /**
     * Selects a contact.
     *
     * @param index The index of the contact to select.
     */
    fun selectContact(index: Int) {
        scope.launch { selectContactInternal(index) }
    }

    /**
     * Cancels the current contact selection.
     */
    fun cancelSelection() {
        scope.launch { cancelSelectionInternal() }
    }

    /**
     * Handles speech during contact selection.
     *
     * @param text The speech text.
     */
    private suspend fun handleSelectionSpeech(text: String) {
        if (isCancel(text)) {
            cancelSelectionInternal()
            inbox.send(InternalEvent.IntentFailed(text))
            return
        }

        val index = parseSelection(text)
        if (index == null) {
            Log.d(TAG, "Ignoring speech during contact selection: '$text'")
            inbox.send(InternalEvent.IntentFailed(text))
            return
        }

        selectContactInternal(index, fromSpeech = true, rawText = text)
    }

    /**
     * Selects a contact. Internal implementation.
     *
     * @param index The index of the contact to select.
     * @param fromSpeech Whether the selection was initiated by speech.
     * @param rawText The raw speech text when initiated by speech.
     */
    private suspend fun selectContactInternal(
        index: Int,
        fromSpeech: Boolean = false,
        rawText: String = "",
    ) {
        val (option, action) = consumeSelection(index) ?: run {
            Log.d(TAG, "Selection $index ignored (no pending selection or out of range)")
            if (fromSpeech) inbox.send(InternalEvent.IntentFailed(rawText))
            return
        }

        val command = commandResolver.createCall(option.name)
        mediator.handleEvent(OverlayEvent.CommandInterpreted(command))
    }

    /**
     * Cancels the current contact selection. Internal implementation.
     */
    private suspend fun cancelSelectionInternal() {
        selectionMutex.withLock {
            mediator.handleEvent(OverlayEvent.SelectionCleared)
        }
    }

    /**
     * Consumes a contact selection.
     *
     * @param index The index of the contact to consume.
     * @return The consumed contact option and the associated action, or null if no selection is pending or out of range.
     */
    private suspend fun consumeSelection(index: Int): Pair<ContactOption, CommandIdentifier?>? = selectionMutex.withLock {
        val options = mediator.mediatorState.value.contactSelection ?: return@withLock null
        val option = options.getOrNull(index - 1) ?: return@withLock null
        val action = mediator.mediatorState.value.contactSelectionAction
        mediator.handleEvent(OverlayEvent.SelectionCleared)
        Pair(option, action)
    }

    /**
     * Checks if the given text is a cancel command.
     *
     * @param text The text to check.
     * @return True if the text is a cancel command, false otherwise.
     */
    private fun isCancel(text: String): Boolean = normalizer.normalize(text) in cancelPhrases

    /**
     * Parses a selection from the given text.
     *
     * @param text The text to parse.
     * @return The parsed selection, or null if no selection could be parsed.
     */
    private fun parseSelection(text: String): Int? {
        val normalized = normalizer.normalize(text)
            .split(" ")
            .joinToString(" ") { extraNumberWords[it] ?: it }
        return selectionRegex.matchEntire(normalized)?.groupValues?.get(1)?.toIntOrNull()
    }

    override fun accept(input: VoiceInput) {
        inbox.trySend(input)
    }

    companion object {
        private const val TAG = "VoiceSessionManager"
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

        private val extraNumberWords = mapOf(
            "jedynkę" to "1",
            "jedynka" to "1",
            "pierwszy" to "1",
            "pierwsza" to "1",
            "pierwsze" to "1",
            "pierwszego" to "1",
            "pierwszą" to "1",
            "dwójkę" to "2",
            "dwójka" to "2",
            "drugi" to "2",
            "druga" to "2",
            "drugie" to "2",
            "drugiego" to "2",
            "drugą" to "2",
            "trójkę" to "3",
            "trójka" to "3",
            "trzeci" to "3",
            "trzecia" to "3",
            "trzecie" to "3",
            "trzeciego" to "3",
            "trzecią" to "3",
            "czwórkę" to "4",
            "czwórka" to "4",
            "czwarty" to "4",
            "czwarta" to "4",
            "czwarte" to "4",
            "czwartego" to "4",
            "czwartą" to "4",
            "piątkę" to "5",
            "piątka" to "5",
            "piąty" to "5",
            "piąta" to "5",
            "piąte" to "5",
            "piątego" to "5",
            "piątą" to "5",
            "szóstkę" to "6",
            "szóstka" to "6",
            "szósty" to "6",
            "szósta" to "6",
            "szóste" to "6",
            "szóstego" to "6",
            "szóstą" to "6",
            "siódemkę" to "7",
            "siódemka" to "7",
            "siódmy" to "7",
            "siódma" to "7",
            "siódme" to "7",
            "siódmego" to "7",
            "siódmą" to "7",
            "ósemkę" to "8",
            "ósemka" to "8",
            "ósmy" to "8",
            "ósma" to "8",
            "ósme" to "8",
            "ósmego" to "8",
            "ósmą" to "8",
            "dziewiątkę" to "9",
            "dziewiątka" to "9",
            "dziewiąty" to "9",
            "dziewiąta" to "9",
            "dziewiąte" to "9",
            "dziewiątego" to "9",
            "dziewiątą" to "9",
            "dziesiątkę" to "10",
            "dziesiątka" to "10",
            "dziesiąty" to "10",
            "dziesiąta" to "10",
            "dziesiąte" to "10",
            "dziesiątego" to "10",
            "dziesiątą" to "10",
        )
    }
}
