package com.zpi.slowomir.overlay.mediator

import android.util.Log
import androidx.compose.runtime.currentComposer
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.events.VoiceEvent
import com.zpi.slowomir.overlay.voice.OverlayState
import com.zpi.slowomir.overlay.voice.OverlayState.*
import com.zpi.slowomir.overlay.voice.VoiceIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Default implementation of [CommandMediator].
 * Coordinates context state and validates/executes commands.
 */
@Singleton
class DefaultCommandMediator @Inject constructor() : CommandMediator {

    private val _mediatorState = MutableStateFlow(MediatorState())
    override val mediatorState: StateFlow<MediatorState> = _mediatorState.asStateFlow()

    override fun updateContext(context: OverlayContext) {
        _mediatorState.update { currentState ->
            currentState.copy(overlayContext = context)
        }
    }

    override suspend fun handleEvent(event: VoiceEvent) {
        Log.i(TAG, "Handling event: $event")

        val state = when (event) {
            is VoiceEvent.WakeWordDetected -> {
                ListeningForWakeWord
            }

            is VoiceEvent.SpeechPartial -> {
                ListeningToSpeech(event.text)
            }

            is VoiceEvent.SpeechRecognized -> {
                ListeningToSpeech(event.text)
            }

            is VoiceEvent.CommandInterpreted -> {
                val result = dispatch(event.command)
                formatCommandResult(event.command, result, mediatorState.value.intent)
            }

            is VoiceEvent.InvalidCommand -> {
                Error("Invalid command: ${event.text}")
            }

            is VoiceEvent.ProcessingCommand -> {
                ProcessingCommand(event.text)
            }

            is VoiceEvent.IntentParsed -> {
                _mediatorState.update {
                    it.copy(intent = event.intent)
                }

                null
            }
        }

        if (state != null) {
            _mediatorState.update {
                it.copy(overlayState = state)
            }
        }
    }

    override suspend fun dispatch(command: Command): CommandResult {
        val context = mediatorState.value.overlayContext

        if (!command.canExecute(context)) {
            val invalidResult = CommandResult.InvalidContext(
                "Command ${command::class.simpleName} cannot execute in current context"
            )

            return invalidResult
        }

        val result = try {
            command.execute()
        } catch (e: Exception) {
            CommandResult.Failure("Unhandled exception during execution of ${command::class.simpleName}:\n ${e.message}")
        }


        return result
    }

    fun formatCommandResult(
        command: Command,
        result: CommandResult,
        intent: VoiceIntent?
    ): OverlayState {
        val intentDesc = intent?.let {
            val argStr = when (val arg = it.argument) {
                is CommandArgument.Number -> " [${arg.value}]"
                is CommandArgument.TwoNumbers -> " [${arg.from} -> ${arg.to}]"
                is CommandArgument.Text -> " ['${arg.value}']"
                null -> ""
            }
            "${it.identifier}$argStr"
        } ?: command::class.simpleName


        return when (result) {
            is CommandResult.Success -> CommandExecuted("$intentDesc via ${command::class.simpleName}")
            is CommandResult.Failure -> Error("${command::class.simpleName} Failed: ${result.reason}")
            is CommandResult.InvalidContext -> Error("Invalid Context: ${result.reason}")
        }
    }

    companion object {
        private const val TAG = "CommandMediator"
    }
}