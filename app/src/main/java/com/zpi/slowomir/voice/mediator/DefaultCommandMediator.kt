package com.zpi.slowomir.voice.mediator

import android.util.Log
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.log.ICommandLogger
import com.zpi.slowomir.voice.OverlayError
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.OverlayState.*
import com.zpi.slowomir.voice.VoiceIntent
import com.zpi.slowomir.voice.stt.TextMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Default implementation of [CommandMediator].
 * Coordinates context state and validates/executes commands.
 */
@Singleton
class DefaultCommandMediator @Inject constructor(
    private val commandLogger: ICommandLogger
) : CommandMediator {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _mediatorState = MutableStateFlow(MediatorState())
    override val mediatorState: StateFlow<MediatorState> = _mediatorState.asStateFlow()

    override fun dispatchScoped(command: Command): Deferred<CommandResult> {
        return scope.async {
            dispatch(command)
        }
    }

    override fun updateContext(context: OverlayContext) {
        _mediatorState.update { currentState ->
            currentState.copy(overlayContext = context)
        }
    }

    override suspend fun handleEvent(event: OverlayEvent) {
        Log.i(TAG, "Handling event: $event")

        val state = when (event) {
            is OverlayEvent.WakeWordDetected -> {
                ListeningForCommand
            }

            is OverlayEvent.SpeechPartial -> speechState(event.text, event.mode)

            is OverlayEvent.SpeechRecognized -> speechState(event.text, event.mode)

            is OverlayEvent.SessionIdle -> {
                if (mediatorState.value.overlayState.isResult) null else ListeningForKeyword
            }

            is OverlayEvent.CommandExecuted -> {
                commandOutcome(event.command, event.result, mediatorState.value.intent)
            }

            is OverlayEvent.InvalidCommand -> {
                Error(OverlayError.InvalidCommand(event.text))
            }

            is OverlayEvent.ProcessingCommand -> {
                ProcessingCommand(event.text)
            }

            is OverlayEvent.IntentParsed -> {
                _mediatorState.update {
                    it.copy(intent = event.intent)
                }

                null
            }

            is OverlayEvent.AwaitingSelection -> {
                Idle
            }

            is OverlayEvent.CommandRejected -> {
                Error(event.error)
            }
        }

        if (state != null) {
            _mediatorState.update {
                it.copy(overlayState = state)
            }
        }
    }

    private fun commandOutcome(command: Command, result: CommandResult, intent: VoiceIntent?): OverlayState {
        val commandName = command::class.simpleName.orEmpty()
        return when (result) {
            is CommandResult.Success -> CommandExecuted(commandName, intent)
            is CommandResult.Failure -> Error(OverlayError.CommandFailed(commandName, result.reason))
            is CommandResult.InvalidContext -> Error(OverlayError.InvalidContext(result.reason))
        }
    }

    private fun speechState(text: String, mode: TextMode?): OverlayState = when (mode) {
        TextMode.DICTATE -> Dictating(text)
        TextMode.EDIT -> Editing(text)
        null -> ListeningToSpeech(text)
    }

    override suspend fun dispatch(command: Command): CommandResult = withContext(Dispatchers.Main.immediate) {
        val context = mediatorState.value.overlayContext

        if (!command.canExecute(context)) {
            val invalidResult = CommandResult.InvalidContext(
                "Command ${command::class.simpleName} cannot execute in current context"
            )

            return@withContext invalidResult
        }

        val result = try {
            val executionResult = if (command.requiresBackgroundExecution) {
                withContext(Dispatchers.Default) { command.execute() }
            } else {
                command.execute()
            }
            executionResult.also {
                if (executionResult is CommandResult.Success) {
                    commandLogger.log(command)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CommandResult.Failure("Unhandled exception during execution of ${command::class.simpleName}:\n ${e.message}")
        }

        result
    }

    companion object {
        private const val TAG = "CommandMediator"
    }
}