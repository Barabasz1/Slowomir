package com.zpi.slowomir.overlay.voice

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.mediator.CommandMediator
import com.zpi.slowomir.overlay.pipeline.CommandPipeline
import com.zpi.slowomir.overlay.pipeline.PipelineStage
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
    private val tapCommandFactory: TapCommand.Factory
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val filterStage = PipelineStage<String, String> { text ->
        if (text.isNotBlank()) text else null
    }

    private val interpreterStage = PipelineStage<String, Command> { text ->
        _uiState.value = VoiceUiState.ProcessingCommand(text)
        interpretTextToCommand(text)
    }

    private val pipeline = CommandPipeline.Builder<String>(mediator)
        .addStage(filterStage)
        .addStage(interpreterStage)
        .build()

    init {
        scope.launch {
            mediator.commandResults.collect { (command, result) ->
                _uiState.value = when (result) {
                    is CommandResult.Success -> VoiceUiState.CommandExecuted("${command::class.simpleName} Succeeded")
                    is CommandResult.Failure -> VoiceUiState.Error(result.reason)
                    is CommandResult.InvalidContext -> VoiceUiState.Error(result.reason)
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

    private fun interpretTextToCommand(text: String): Command? {
        return if (text.contains("tap", ignoreCase = true) || text.contains("click", ignoreCase = true)) {
            tapCommandFactory.create(500f, 500f)
        } else {
            null
        }
    }
}
