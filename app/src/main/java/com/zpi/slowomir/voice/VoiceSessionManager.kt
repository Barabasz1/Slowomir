package com.zpi.slowomir.voice

import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.LockScreenCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateBackCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateRecentCommand
import com.zpi.slowomir.overlay.commands.navigation.NotificationsCommand
import com.zpi.slowomir.overlay.commands.navigation.QuickSettingsCommand
import com.zpi.slowomir.overlay.commands.navigation.ScreenshotCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeDownCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeLeftCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeRightCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.ZoomInCommand
import com.zpi.slowomir.overlay.commands.navigation.ZoomOutCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.CallCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.DeclineCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.EndCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.PickUpCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.SpeakerCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.WriteSMSCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.pipeline.Pipeline
import com.zpi.slowomir.voice.pipeline.PipelineStage
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

@Singleton
class VoiceSessionManager @Inject constructor(
    private val mediator: CommandMediator,
    private val intentParser: CommandIntentParser,
    private val normalizer: PolishTextNormalizer,
    private val callCommandFactory: CallCommand.Factory,
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


    private val filterStage = PipelineStage<String, String> { text ->
        if (text.isNotBlank()) text else null
    }

    private val intentStage = PipelineStage<String, VoiceIntent> { text ->
        scope.launch {
            mediator.handleEvent(OverlayEvent.ProcessingCommand(text))
        }

        val intent = intentParser.parse(text)

        if (intent != null) {
            scope.launch {
                mediator.handleEvent(OverlayEvent.IntentParsed(intent))
            }
        }

        intent
    }

    private val commandStage = PipelineStage<VoiceIntent, Command> { intent ->
        commandResolver.resolve(intent)
    }

    private val pipeline = Pipeline.Builder.create<String>()
        .addStage(filterStage)
        .addStage(intentStage)
        .addStage(commandStage)
        .build()


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

    private fun onStateChanged(newState: VoiceState) {
        when (newState) {
            is VoiceState.IntentParsing -> {
                parseIntent(newState.rawText)
            }

            is VoiceState.CommandExecuting -> {
                executeCommand(newState.intent, newState.command)
            }

            else -> {}
        }
    }

    private fun parseIntent(text: String) {
        scope.launch {
            mediator.handleEvent(OverlayEvent.ProcessingCommand(text))

            if (mediator.mediatorState.value.contactSelection != null) {
                handleSelectionSpeech(text)
                return@launch
            }

            val command = pipeline.process(text)
            val intent = intentParser.parse(text)

            if (command != null && intent != null) {
                inbox.send(InternalEvent.IntentParsed(intent, command))
            } else {
                mediator.handleEvent(OverlayEvent.InvalidCommand(text))
                inbox.send(InternalEvent.IntentFailed(text))
            }
        }
    }

    private fun executeCommand(intent: VoiceIntent, command: Command) {
        scope.launch {
            mediator.handleEvent(OverlayEvent.CommandInterpreted(command))

            val result = mediator.dispatch(command)
            val returnToKws = intent.identifier == CommandIdentifier.OVERLAY_STOP

            inbox.send(InternalEvent.CommandFinished(result, returnToKws = returnToKws))
        }
    }

    fun selectContact(index: Int) {
        scope.launch { selectContactInternal(index) }
    }

    fun cancelSelection() {
        scope.launch { cancelSelectionInternal() }
    }

    private suspend fun handleSelectionSpeech(text: String) {
        if (isCancel(text)) {
            cancelSelectionInternal()
            return
        }

        val index = parseSelection(text)
        if (index == null) {
            Log.d(TAG, "Ignoring speech during contact selection: '$text'")
            return
        }

        selectContactInternal(index)
    }

    private suspend fun selectContactInternal(index: Int) {
        val option = consumeSelection(index)
        if (option == null) {
            Log.d(TAG, "Selection $index ignored (no pending selection or out of range)")
            return
        }

        val command = commandResolver.createCall(option.name)
        mediator.handleEvent(OverlayEvent.CommandInterpreted(command))
    }

    private suspend fun cancelSelectionInternal() {
        selectionMutex.withLock {
            mediator.handleEvent(OverlayEvent.SelectionCleared)
        }
    }

    private suspend fun consumeSelection(index: Int): ContactOption? = selectionMutex.withLock {
        val options = mediator.mediatorState.value.contactSelection ?: return@withLock null
        val option = options.getOrNull(index - 1) ?: return@withLock null
        mediator.handleEvent(OverlayEvent.SelectionCleared)
        option
    }

    private fun isCancel(text: String): Boolean = normalizer.normalize(text) in cancelPhrases

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
        )

        private val extraNumberWords = mapOf(
            "jedynkę" to "1",
            "dwójkę" to "2",
            "trójkę" to "3",
            "czwórkę" to "4",
            "piątkę" to "5",
        )
    }
}
