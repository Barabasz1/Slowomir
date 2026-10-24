package com.zpi.slowomir.overlay.voice

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.mediator.CommandMediator
import com.zpi.slowomir.overlay.pipeline.CommandPipeline
import com.zpi.slowomir.overlay.pipeline.PipelineStage
import com.zpi.slowomir.overlay.services.CommandExecutorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Session Manager coordinating speech recognition state,
 * routing transcribed text through a [CommandPipeline], and dispatching resulting commands
 * to the [CommandMediator].
 */
@Singleton
class VoiceSessionManager @Inject constructor(
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val intentParser: CommandIntentParser
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    @Volatile
    private var lastParsedIntent: VoiceIntent? = null

    private val filterStage = PipelineStage<String, String> { text ->
        if (text.isNotBlank()) text else null
    }

    private val intentStage = PipelineStage<String, VoiceIntent> { text ->
        _uiState.value = VoiceUiState.ProcessingCommand(text)
        val intent = intentParser.parse(text)
        if (intent == null) {
            _uiState.value = VoiceUiState.Error("Nierozpoznano komendy: '$text'")
        }
        lastParsedIntent = intent
        intent
    }

    private val commandStage = PipelineStage<VoiceIntent, Command> { intent ->
        resolveIntentToCommand(intent)
    }

    private val pipeline = CommandPipeline.Builder<String>(mediator)
        .addStage(filterStage)
        .addStage(intentStage)
        .addStage(commandStage)
        .build()

    init {
        scope.launch {
            mediator.commandResults.collect { (command, result) ->
                val intentDesc = lastParsedIntent?.let {
                    val argStr = when (val arg = it.argument) {
                        is CommandArgument.Number -> " [${arg.value}]"
                        is CommandArgument.TwoNumbers -> " [${arg.from} -> ${arg.to}]"
                        is CommandArgument.Text -> " ['${arg.value}']"
                        null -> ""
                    }
                    "${it.identifier}$argStr"
                } ?: command::class.simpleName

                _uiState.value = when (result) {
                    is CommandResult.Success -> VoiceUiState.CommandExecuted("$intentDesc via ${command::class.simpleName}")
                    is CommandResult.Failure -> VoiceUiState.Error("${command::class.simpleName} Failed: ${result.reason}")
                    is CommandResult.InvalidContext -> VoiceUiState.Error("Invalid Context: ${result.reason}")
                }
            }
        }
    }

    /**
     * Called when a wake word is detected to update UI state to listening mode.
     */
    fun onWakeWordDetected() {
        _uiState.value = VoiceUiState.ListeningForWakeWord
    }

    /**
     * Called when speech recognition produces results.
     *
     * @param text The transcribed speech string.
     * @param isFinal True if the speech segment is complete and ready for command processing.
     */
    fun onSpeechResult(text: String, isFinal: Boolean) {
        if (!isFinal) {
            _uiState.value = VoiceUiState.ListeningToSpeech(text)
        } else {
            scope.launch {
                pipeline.execute(text)
            }
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
            CommandIdentifier.CELL_PRESS -> {
                val (x, y) = when (val arg = intent.argument) {
                    is CommandArgument.Number -> cellNumberToCoordinates(arg.value)
                    else -> Pair(DEFAULT_TAP_X, DEFAULT_TAP_Y)
                }
                tapCommandFactory.create(x, y)
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
