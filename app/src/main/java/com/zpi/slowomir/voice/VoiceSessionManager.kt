package com.zpi.slowomir.voice

import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.voice.intent.CommandIntentParser
import com.zpi.slowomir.voice.intent.CommandIntentResolver
import com.zpi.slowomir.voice.intent.IntentResolution
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.selection.SelectionResult
import com.zpi.slowomir.voice.selection.Selection
import com.zpi.slowomir.voice.selection.SelectionManager
import com.zpi.slowomir.voice.selection.SelectionSpeechResult
import com.zpi.slowomir.voice.stt.InternalEvent
import com.zpi.slowomir.voice.stt.SessionEvent
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceInputSink
import com.zpi.slowomir.voice.stt.VoiceState
import com.zpi.slowomir.voice.stt.TextMode
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
import com.zpi.slowomir.overlay.commands.textEdit.DictateCommand
import com.zpi.slowomir.voice.dictation.FieldProfileHolder
import com.zpi.slowomir.voice.dictation.PunctuationRestorer
import com.zpi.slowomir.voice.mediator.OverlayEvent
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice session manager. Coordinates the state of the voice session and dispatches commands.
 */
@Singleton
class VoiceSessionManager @Inject constructor(
    private val mediator: CommandMediator,
    private val intentParser: CommandIntentParser,
    private val commandResolver: CommandResolver,
    private val selectionManager: SelectionManager,
    private val intentResolver: CommandIntentResolver,
    private val normalizer: PolishTextNormalizer,
    private val dictateCommandFactory: DictateCommand.Factory,
    private val punctuationRestorer: PunctuationRestorer,
    private val fieldProfileHolder: FieldProfileHolder,
) : VoiceInputSink, VoiceStateSource {

    val selection: StateFlow<Selection?> = selectionManager.selection

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Stopped)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val inbox = Channel<SessionEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            for (input in inbox) handle(input)
        }
        scope.launch {
            intentResolver.initialize().onFailure {
                Log.w(TAG, "Intent model unavailable, using the rule-based parser only", it)
            }
        }
    }

    private suspend fun handle(input: SessionEvent) {
        when (input) {
            is VoiceInput.SpeechPartial -> {
                mediator.handleEvent(OverlayEvent.SpeechPartial(input.text, _state.value.textMode()))
            }

            is VoiceInput.KeywordDetected -> {
                mediator.handleEvent(OverlayEvent.WakeWordDetected)
            }

            is InternalEvent.SelectionPicked -> {
                selectionManager.choose(input.index)?.let { proceed(it) }
            }

            is InternalEvent.SelectionCancelled -> {
                selectionManager.cancel()
            }

            else -> {}
        }

        val oldState = _state.value
        val newState = VoiceStateMachine.reduce(oldState, input)
        _state.value = newState

        if (oldState != newState) {
            Log.d(TAG, "State transition: $oldState + $input -> $newState")
            onStateChanged(newState)
            updatePunctuationModel(oldState, newState)
            if (newState.isIdle()) mediator.handleEvent(OverlayEvent.SessionIdle)
        }
    }

    private fun VoiceState.isIdle() =
        this is VoiceState.Stopped || this is VoiceState.Failed || this is VoiceState.KeywordSpotting

    private fun VoiceState.textMode(): TextMode? = when (this) {
        is VoiceState.Dictating -> TextMode.DICTATE
        is VoiceState.Editing -> TextMode.EDIT
        is VoiceState.TextProcessing -> mode
        else -> null
    }

    /**
     * Keeps the punctuation model in memory only while dictating: it is loaded when dictation starts
     * and released when it ends (exit command, keyword spotting, service stopped).
     */
    private fun updatePunctuationModel(oldState: VoiceState, newState: VoiceState) {
        val wasDictating = oldState.isTextSession()
        val isDictating = newState.isTextSession()
        when {
            !wasDictating && isDictating -> scope.launch {
                punctuationRestorer.initialize().onFailure {
                    Log.w(TAG, "Punctuation model unavailable, dictation will be typed as recognized", it)
                }
            }

            wasDictating && !isDictating -> punctuationRestorer.release()
        }
    }

    private fun VoiceState.isTextSession() =
        this is VoiceState.Dictating || this is VoiceState.Editing || this is VoiceState.TextProcessing

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

            is VoiceState.TextProcessing -> {
                handleTextInput(newState.rawText, newState.mode)
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

            Log.d(TAG, "Parsing '$text' (selection active: ${selectionManager.isActive})")

            if (selectionManager.isActive) {
                when (val result = selectionManager.handleSpeech(text)) {
                    is SelectionSpeechResult.Chosen -> proceed(result.intent)
                    is SelectionSpeechResult.Cancelled -> inbox.send(InternalEvent.IntentFailed(result.rawText))
                    is SelectionSpeechResult.Ignored -> inbox.send(InternalEvent.IntentFailed(result.rawText))
                }
                return@launch
            }

            val intent = intentParser.parse(text) ?: resolveWithModel(text)

            if (intent == null) {
                mediator.handleEvent(OverlayEvent.InvalidCommand(text))
                inbox.send(InternalEvent.IntentFailed(text))
                return@launch
            }

            if (intent.identifier in SELECTABLE_INTENTS) {
                when (val result = selectionManager.begin(intent)) {
                    is SelectionResult.Resolved -> proceed(result.intent)
                    is SelectionResult.Pending -> {
                        mediator.handleEvent(OverlayEvent.AwaitingSelection)
                        inbox.send(InternalEvent.IntentFailed(text))
                    }
                    is SelectionResult.Failed -> {
                        mediator.handleEvent(OverlayEvent.CommandRejected(OverlayError.SelectionFailed(result.failure)))
                        inbox.send(InternalEvent.IntentFailed(text))
                    }
                }
                return@launch
            }

            proceed(intent)
        }
    }

    /**
     * Handles speech inside a text input session. Only the commands that belong to the current [mode] are
     * recognized: in dictation mode the rest is typed into the field, in edit mode anything else is rejected.
     */
    private fun handleTextInput(text: String, mode: TextMode) {
        scope.launch {
            mediator.handleEvent(OverlayEvent.SpeechRecognized(text, mode))
            val allowed = if (mode == TextMode.EDIT) EDIT_MODE_COMMANDS else DICTATION_MODE_COMMANDS

            val spoken = intentParser.parse(text) ?: if (mode == TextMode.EDIT) resolveWithModel(text) else null
            val intent = spoken?.takeIf { it.identifier in allowed }
            when {
                intent != null -> proceed(intent)

                mode == TextMode.EDIT -> {
                    mediator.handleEvent(OverlayEvent.InvalidCommand(text))
                    inbox.send(InternalEvent.TextHandled)
                }

                else -> {
                    dictate(text)
                    inbox.send(InternalEvent.TextHandled)
                }
            }
        }
    }

    /**
     * Restores punctuation and casing in recognized speech and types it into the focused text field.
     */
    private suspend fun dictate(text: String) {
        val profile = fieldProfileHolder.profile
        Log.d(TAG, "[DICTATION] raw='$text' profile=$profile")
        var restored = punctuationRestorer.restore(text, profile.punctuation)
        Log.d(TAG, "[DICTATION] restored='$restored'")
        if (profile.uppercaseAll) restored = restored.uppercase(POLISH)
        val result = mediator.dispatch(dictateCommandFactory.create(restored))
        Log.d(TAG, "Dictated '$text' -> '$restored': $result")
        if (result !is CommandResult.Success) mediator.handleEvent(OverlayEvent.CommandRejected(OverlayError.TextInsertFailed))
    }

    /**
     * Fallback for utterances the rule-based parser rejects: asks the intent model for the
     * command and its argument. Returns null when the model is unsure, unavailable, or did not
     * extract the argument that command needs.
     */
    private suspend fun resolveWithModel(text: String): VoiceIntent? {
        val result = intentResolver.resolve(normalizer.normalize(text))
        if (result !is IntentResolution.Resolved) {
            Log.d(TAG, "Model could not resolve '$text': $result")
            return null
        }

        val intent = VoiceIntent(result.identifier, result.argument, text)
        if (!hasRequiredArgument(intent)) {
            Log.d(TAG, "Model resolved ${result.identifier} for '$text' without a usable argument")
            return null
        }
        Log.d(TAG, "Model resolved '$text' -> ${result.identifier} (${result.confidence})")
        return intent
    }

    private fun hasRequiredArgument(intent: VoiceIntent): Boolean = when (intent.identifier) {
        CommandIdentifier.CELL_SELECT -> intent.argument is CommandArgument.Number
        CommandIdentifier.CELL_SWIPE -> intent.argument is CommandArgument.TwoNumbers
        CommandIdentifier.NAVIGATE_APP_OPEN,
        CommandIdentifier.PHONE_CALL,
        CommandIdentifier.SMS_WRITE,
        CommandIdentifier.MACRO_PLAY -> intent.argument is CommandArgument.Text
        else -> true
    }

    /**
     * Turns a complete intent into a command and moves the session on to executing it.
     */
    private suspend fun proceed(intent: VoiceIntent) {
        inbox.send(InternalEvent.IntentParsed(intent, commandResolver.resolve(intent)))
    }

    /**
     * Executes a command.
     *
     * @param intent The command intent.
     * @param command The command to execute.
     */
    private fun dispatchCommand(intent: VoiceIntent, command: Command) {
        scope.launch {
            val result = mediator.dispatch(command)
            mediator.handleEvent(OverlayEvent.CommandExecuted(command, result))
            val returnToKws = intent.identifier == CommandIdentifier.OVERLAY_STOP

            inbox.send(InternalEvent.CommandFinished(result, returnToKws = returnToKws))
        }
    }

    /**
     * Picks an option of the pending selection.
     *
     * @param index The 1-based index of the chosen option.
     */
    fun selectOption(index: Int) {
        inbox.trySend(InternalEvent.SelectionPicked(index))
    }

    /**
     * Cancels the pending selection.
     */
    fun cancelSelection() {
        inbox.trySend(InternalEvent.SelectionCancelled)
    }

    override fun accept(input: VoiceInput) {
        inbox.trySend(input)
    }

    companion object {
        private const val TAG = "VoiceSessionManager"

        private val POLISH = Locale.forLanguageTag("pl-PL")

        private val DICTATION_MODE_COMMANDS = setOf(
            CommandIdentifier.TEXT_EDIT,
            CommandIdentifier.TEXT_CONFIRM,
            CommandIdentifier.TEXT_CANCEL,
            CommandIdentifier.OVERLAY_STOP,
        )

        private val EDIT_MODE_COMMANDS = setOf(
            CommandIdentifier.TEXT_ENTER,
            CommandIdentifier.TEXT_CURSOR_FORWARD,
            CommandIdentifier.TEXT_CURSOR_BACKWARD,
            CommandIdentifier.TEXT_CURSOR_NEXT_WORD,
            CommandIdentifier.TEXT_CURSOR_PREVIOUS_WORD,
            CommandIdentifier.TEXT_DELETE_CHARACTER,
            CommandIdentifier.TEXT_DELETE_WORD,
            CommandIdentifier.TEXT_CONFIRM,
            CommandIdentifier.OVERLAY_STOP,
        )

        private val SELECTABLE_INTENTS = setOf(
            CommandIdentifier.PHONE_CALL,
            CommandIdentifier.SMS_WRITE,
            CommandIdentifier.NAVIGATE_APP_OPEN,
            CommandIdentifier.MACRO_SHOW,
        )
    }
}
