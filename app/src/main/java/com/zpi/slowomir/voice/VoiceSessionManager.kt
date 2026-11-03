package com.zpi.slowomir.voice

import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.TapCommand
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
    private val tapCommandFactory: TapCommand.Factory,
    private val notificationsCommandFactory: NotificationsCommand.Factory,
    private val quickSettingsCommandFactory: QuickSettingsCommand.Factory,
    private val lockScreenCommandFactory: LockScreenCommand.Factory,
    private val screenshotCommandFactory: ScreenshotCommand.Factory,
    private val navigateHomeCommandFactory: NavigateHomeCommand.Factory,
    private val navigateRecentCommandFactory: NavigateRecentCommand.Factory,
    private val keyboardHideCommandFactory: KeyboardHideCommand.Factory,
    private val keyboardShowCommandFactory: KeyboardShowCommand.Factory,
    private val pickUpCommandFactory: PickUpCommand.Factory,
    private val declineCommandFactory: DeclineCommand.Factory,
    private val endCommandFactory: EndCommand.Factory,
    private val speakerCommandFactory: SpeakerCommand.Factory,
    private val callCommandFactory: CallCommand.Factory,
    private val writeSMSCommandFactory: WriteSMSCommand.Factory,
    private val navigateBackCommandFactory: NavigateBackCommand.Factory,
    private val swipeUpCommandFactory: SwipeUpCommand.Factory,
    private val swipeDownCommandFactory: SwipeDownCommand.Factory,
    private val swipeLeftCommandFactory: SwipeLeftCommand.Factory,
    private val swipeRightCommandFactory: SwipeRightCommand.Factory,
    private val zoomInCommandFactory: ZoomInCommand.Factory,
    private val zoomOutCommandFactory: ZoomOutCommand.Factory,
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
            mediator.handleEvent(VoiceEvent.ProcessingCommand(text))
        }

        val intent = intentParser.parse(text)

        if (intent != null) {
            scope.launch {
                mediator.handleEvent(VoiceEvent.IntentParsed(intent))
            }
        }

        intent
    }

    private val commandStage = PipelineStage<VoiceIntent, Command> { intent ->
        resolveIntentToCommand(intent)
    }

    private val pipeline = Pipeline.Builder.create<String>()
        .addStage(filterStage)
        .addStage(intentStage)
        .addStage(commandStage)
        .build()


    private suspend fun handle(input: SessionEvent) {
        when (input) {
            is VoiceInput.SpeechPartial -> {
                mediator.handleEvent(VoiceEvent.SpeechPartial(input.text))
            }

            is VoiceInput.KeywordDetected -> {
                mediator.handleEvent(VoiceEvent.WakeWordDetected)
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
            mediator.handleEvent(VoiceEvent.ProcessingCommand(text))

            if (mediator.mediatorState.value.contactSelection != null) {
                handleSelectionSpeech(text)
                return@launch
            }

            val command = pipeline.process(text)
            val intent = intentParser.parse(text)

            if (command != null && intent != null) {
                inbox.send(InternalEvent.IntentParsed(intent, command))
            } else {
                mediator.handleEvent(VoiceEvent.InvalidCommand(text))
                inbox.send(InternalEvent.IntentFailed(text))
            }
        }
    }

    private fun executeCommand(intent: VoiceIntent, command: Command) {
        scope.launch {
            mediator.handleEvent(VoiceEvent.CommandInterpreted(command))

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

        val command = callCommandFactory.create(recipient = option.number)
        mediator.handleEvent(VoiceEvent.CommandInterpreted(command))
    }

    private suspend fun cancelSelectionInternal() {
        selectionMutex.withLock {
            mediator.handleEvent(VoiceEvent.SelectionCleared)
        }
    }

    private suspend fun consumeSelection(index: Int): ContactOption? = selectionMutex.withLock {
        val options = mediator.mediatorState.value.contactSelection ?: return@withLock null
        val option = options.getOrNull(index - 1) ?: return@withLock null
        mediator.handleEvent(VoiceEvent.SelectionCleared)
        option
    }

    private fun isCancel(text: String): Boolean = normalizer.normalize(text) in cancelPhrases

    private fun parseSelection(text: String): Int? {
        val normalized = normalizer.normalize(text)
            .split(" ")
            .joinToString(" ") { extraNumberWords[it] ?: it }
        return selectionRegex.matchEntire(normalized)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun resolveIntentToCommand(intent: VoiceIntent): Command {
        return when (intent.identifier) {
            // Navigation commands
            CommandIdentifier.CELL_PRESS -> {
                val (x, y) = when (val arg = intent.argument) {
                    is CommandArgument.Number -> cellNumberToCoordinates(arg.value)
                    else -> Pair(DEFAULT_TAP_X, DEFAULT_TAP_Y)
                }
                tapCommandFactory.create(x, y)
            }

            CommandIdentifier.NOTIFICATIONS -> {
                notificationsCommandFactory.create()
            }

            CommandIdentifier.QUICK_SETTINGS -> {
                quickSettingsCommandFactory.create()
            }

            CommandIdentifier.LOCK_SCREEN -> {
                lockScreenCommandFactory.create()
            }

            CommandIdentifier.SCREENSHOT -> {
                screenshotCommandFactory.create()
            }

            CommandIdentifier.NAVIGATE_HOME -> {
                navigateHomeCommandFactory.create()
            }

            CommandIdentifier.NAVIGATE_RECENT -> {
                navigateRecentCommandFactory.create()
            }

            CommandIdentifier.NAVIGATE_BACK -> {
                navigateBackCommandFactory.create()
            }
            CommandIdentifier.SWIPE_UP -> {
                val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
                val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
                val x = screenWidth / 2f
                val y = screenHeight * 0.8f
                swipeUpCommandFactory.create(x, y)
            }
            CommandIdentifier.SWIPE_DOWN -> {
                val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
                val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
                val x = screenWidth / 2f
                val y = screenHeight * 0.2f
                swipeDownCommandFactory.create(x, y)
            }
            CommandIdentifier.SWIPE_LEFT -> {
                val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
                val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
                val x = screenWidth * 0.8f
                val y = screenHeight / 2f
                swipeLeftCommandFactory.create(x, y)
            }
            CommandIdentifier.SWIPE_RIGHT -> {
                val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
                val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
                val x = screenWidth * 0.2f
                val y = screenHeight / 2f
                swipeRightCommandFactory.create(x, y)
            }
            CommandIdentifier.ZOOM_IN -> {
                val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
                val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
                val x = screenWidth / 2f
                val y = screenHeight / 2f
                zoomInCommandFactory.create(x, y)
            }
            CommandIdentifier.ZOOM_OUT -> {
                val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
                val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
                val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT
                val x = screenWidth / 2f
                val y = screenHeight / 2f
                zoomOutCommandFactory.create(x, y)
            }

            // Text editing commands
            CommandIdentifier.HIDE_KEYBOARD -> {
                keyboardHideCommandFactory.create()
            }

            CommandIdentifier.SHOW_KEYBOARD -> {
                keyboardShowCommandFactory.create()
            }

            // Phone call commands
            CommandIdentifier.PHONE_PICK_UP -> {
                pickUpCommandFactory.create()
            }

            CommandIdentifier.PHONE_DECLINE -> {
                declineCommandFactory.create()
            }

            CommandIdentifier.PHONE_END_CALL -> {
                endCommandFactory.create()
            }

            CommandIdentifier.PHONE_SPEAKER -> {
                speakerCommandFactory.create()
            }

            CommandIdentifier.PHONE_CALL -> {
                val recipient = when (val arg = intent.argument) {
                    is CommandArgument.Text -> arg.value
                    else -> null
                }
                callCommandFactory.create(recipient = recipient)
            }

            CommandIdentifier.SMS_WRITE -> {
                val recipient = when (val arg = intent.argument) {
                    is CommandArgument.Text -> arg.value
                    else -> null
                }
                writeSMSCommandFactory.create(recipient = recipient)
            }

            else -> LogCommand(intent.identifier, intent.argument)
        }
    }

    /**
     * Dynamic mapping from a cell number to screen coordinates based on active screen metrics.
     * Assumes a 4-column, 5-row grid covering the full screen area.
     *
     * @param cellNumber The 1-based cell number to map.
     * @return A pair of (x, y) coordinates representing the center of the cell.
     */
    private fun cellNumberToCoordinates(cellNumber: Int): Pair<Float, Float> {
        val displayMetrics = CommandExecutorService.instance?.resources?.displayMetrics
        val screenWidth = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
        val screenHeight = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT

        val columns = 4
        val rows = 5
        val cellWidth = screenWidth / columns
        val cellHeight = screenHeight / rows

        val zeroIndexed = (cellNumber - 1).coerceAtLeast(0)
        val col = zeroIndexed % columns
        val row = zeroIndexed / columns

        val x = col * cellWidth + cellWidth / 2f
        val y = row * cellHeight + cellHeight / 2f
        return Pair(x.coerceIn(0f, screenWidth), y.coerceIn(0f, screenHeight))
    }

    override fun accept(input: VoiceInput) {
        inbox.trySend(input)
    }

    companion object {
        private const val TAG = "VoiceSessionManager"

        private const val DEFAULT_SCREEN_WIDTH = 1080f
        private const val DEFAULT_SCREEN_HEIGHT = 2400f
        private const val DEFAULT_TAP_X = 540f
        private const val DEFAULT_TAP_Y = 1200f

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
