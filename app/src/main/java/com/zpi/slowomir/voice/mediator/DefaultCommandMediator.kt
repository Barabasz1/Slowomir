package com.zpi.slowomir.voice.mediator

import android.util.Log
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.phoneCalls.CallCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.WriteSMSCommand
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.OverlayState.*
import com.zpi.slowomir.voice.OverlayEvent
import com.zpi.slowomir.voice.VoiceIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
class DefaultCommandMediator @Inject constructor() : CommandMediator {

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

            is OverlayEvent.SpeechPartial -> {
                ListeningToSpeech(event.text)
            }

            is OverlayEvent.SpeechRecognized -> {
                ListeningToSpeech(event.text)
            }

            is OverlayEvent.CommandInterpreted -> {
                val result = dispatch(event.command)
                if (result is CommandResult.NeedsSelection) {
                    val action = when (event.command) {
                        is WriteSMSCommand -> CommandIdentifier.SMS_WRITE
                        is CallCommand -> CommandIdentifier.PHONE_CALL
                        else -> _mediatorState.value.intent?.identifier
                    }
                    _mediatorState.update {
                        it.copy(
                            contactSelection = result.options,
                            contactSelectionAction = action,
                        )
                    }
                }
                formatCommandResult(event.command, result, mediatorState.value.intent)
            }

            is OverlayEvent.InvalidCommand -> {
                Error("Invalid command: ${event.text}")
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

            is OverlayEvent.SelectionCleared -> {
                _mediatorState.update {
                    it.copy(
                        contactSelection = null,
                        contactSelectionAction = null
                    )
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

    /**
     * Formats a [CommandResult] into an [OverlayState] for display.
     *
     * @param command The command that was executed.
     * @param result The result of the command execution.
     * @param intent The voice intent associated with the command.
     * @return The formatted [OverlayState] for display.
     */
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
            is CommandResult.NeedsSelection -> Idle
        }
    }

    companion object {
        private const val TAG = "CommandMediator"
    }
}