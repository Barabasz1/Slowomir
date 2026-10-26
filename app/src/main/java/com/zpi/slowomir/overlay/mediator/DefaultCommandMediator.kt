package com.zpi.slowomir.overlay.mediator

import android.util.Log
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.events.VoiceEvent
import com.zpi.slowomir.overlay.voice.OverlayState.*
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

    private val _commandResults =
        MutableSharedFlow<Pair<Command, CommandResult>>(extraBufferCapacity = 64)
    override val commandResults: SharedFlow<Pair<Command, CommandResult>> =
        _commandResults.asSharedFlow()

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
                when (val result = dispatch(event.command)) {
                    is CommandResult.Success -> CommandExecuted("${event.command::class.simpleName} Succeeded")
                    is CommandResult.Failure -> Error(result.reason)
                    is CommandResult.InvalidContext -> Error(result.reason)
                }
            }

            is VoiceEvent.InvalidCommand -> {
                Error("Invalid command: ${event.text}")
            }

            is VoiceEvent.ProcessingCommand -> {
                ProcessingCommand(event.text)
            }
        }

        _mediatorState.update { currentState ->
            currentState.copy(overlayState = state)
        }
    }

    override suspend fun dispatch(command: Command): CommandResult {
        val context = mediatorState.value.overlayContext

        if (!command.canExecute(context)) {
            val invalidResult = CommandResult.InvalidContext(
                "Command ${command::class.simpleName} cannot execute in current context"
            )

            _commandResults.emit(command to invalidResult)
            return invalidResult
        }

        val result = try {
            command.execute()
        } catch (e: Exception) {
            CommandResult.Failure("Unhandled exception during execution of ${command::class.simpleName}:\n ${e.message}")
        }

        _commandResults.emit(command to result)
        return result
    }

    companion object {
        private const val TAG = "CommandMediator"
    }
}
