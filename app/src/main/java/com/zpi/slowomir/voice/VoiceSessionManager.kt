package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.navigation.LockScreenCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateRecentCommand
import com.zpi.slowomir.overlay.commands.navigation.NotificationsCommand
import com.zpi.slowomir.overlay.commands.navigation.QuickSettingsCommand
import com.zpi.slowomir.overlay.commands.navigation.ScreenshotCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.pipeline.Pipeline
import com.zpi.slowomir.voice.pipeline.PipelineStage
import com.zpi.slowomir.overlay.services.CommandExecutorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Session Manager coordinating speech recognition state,
 * routing transcribed text through a [Pipeline], and dispatching resulting commands
 * to the [CommandMediator].
 */
@Singleton
class VoiceSessionManager @Inject constructor(
    private val mediator: CommandMediator,
    private val intentParser: CommandIntentParser,
    private val tapCommandFactory: TapCommand.Factory,
    private val notificationsCommandFactory: NotificationsCommand.Factory,
    private val quickSettingsCommandFactory: QuickSettingsCommand.Factory,
    private val lockScreenCommandFactory: LockScreenCommand.Factory,
    private val screenshotCommandFactory: ScreenshotCommand.Factory,
    private val navigateHomeCommandFactory: NavigateHomeCommand.Factory,
    private val navigateRecentCommandFactory: NavigateRecentCommand.Factory,
    private val keyboardHideCommandFactory: KeyboardHideCommand.Factory,
    private val keyboardShowCommandFactory: KeyboardShowCommand.Factory,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    /**
     * Called when a wake word is detected.
     */
    fun onWakeWordDetected() {
        scope.launch {
            mediator.handleEvent(VoiceEvent.WakeWordDetected)
        }
    }

    /**
     * Called when speech recognition produces results.
     *
     * @param text The transcribed speech string.
     */
    fun onSpeechResult(text: String) {
        scope.launch {
            val command = pipeline.process(text)

            if (command != null) {
                mediator.handleEvent(VoiceEvent.CommandInterpreted(command))
            } else {
                mediator.handleEvent(VoiceEvent.InvalidCommand(text))
            }
        }
    }

    /**
     * Called when speech recognition produces partial results.
     *
     * @param text The transcribed speech string.
     */
    fun onSpeechPartial(text: String) {
        scope.launch {
            mediator.handleEvent(VoiceEvent.SpeechPartial(text))
        }
    }

    /**
     * Resolves a voice intent to an executable command.
     *
     * @param intent The voice intent to resolve.
     * @return The corresponding command.
     */
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

            // Text editing commands
            CommandIdentifier.HIDE_KEYBOARD -> {
                keyboardHideCommandFactory.create()
            }
            CommandIdentifier.SHOW_KEYBOARD -> {
                keyboardShowCommandFactory.create()
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

    companion object {
        private const val DEFAULT_SCREEN_WIDTH = 1080f
        private const val DEFAULT_SCREEN_HEIGHT = 2400f
        private const val DEFAULT_TAP_X = 540f
        private const val DEFAULT_TAP_Y = 1200f
    }
}
