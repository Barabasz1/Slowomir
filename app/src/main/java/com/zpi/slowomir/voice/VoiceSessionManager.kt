package com.zpi.slowomir.voice

import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.CommandResult
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
    private val contactSelectionManager: ContactSelectionManager,
) : VoiceInputSink, VoiceStateSource {

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Stopped)
    override val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val inbox = Channel<SessionEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
                when (val result = contactSelectionManager.handleSpeech(text)) {
                    is ContactSelectionSpeechResult.Selected -> {
                        inbox.send(InternalEvent.IntentParsed(result.intent, result.command))
                    }

                    is ContactSelectionSpeechResult.Cancelled -> {
                        inbox.send(InternalEvent.IntentFailed(result.rawText))
                    }

                    is ContactSelectionSpeechResult.Unrecognized -> {
                        inbox.send(InternalEvent.IntentFailed(result.rawText))
                    }
                }
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

            inbox.send(
                InternalEvent.CommandFinished(
                    CommandResult.Success(),
                    returnToKws = returnToKws
                )
            )
        }
    }

    /**
     * Selects a contact (delegates to [ContactSelectionManager]).
     *
     * @param index The index of the contact to select.
     */
    fun selectContact(index: Int) {
        contactSelectionManager.selectContact(index)
    }

    /**
     * Cancels the current contact selection (delegates to [ContactSelectionManager]).
     */
    fun cancelSelection() {
        contactSelectionManager.cancelSelection()
    }

    override fun accept(input: VoiceInput) {
        inbox.trySend(input)
    }

    companion object {
        private const val TAG = "VoiceSessionManager"
    }
}
