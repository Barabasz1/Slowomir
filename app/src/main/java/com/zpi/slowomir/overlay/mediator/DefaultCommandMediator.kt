package com.zpi.slowomir.overlay.mediator

import android.util.Log
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [CommandMediator].
 * Coordinates context state and validates/executes commands.
 */
@Singleton
class DefaultCommandMediator @Inject constructor() : CommandMediator {

    private val _currentContext = MutableStateFlow(OverlayContext(packageName = "", isTextFieldFocused = false))
    override val currentContext: StateFlow<OverlayContext> = _currentContext.asStateFlow()

    private val _commandStream = MutableSharedFlow<Command>(extraBufferCapacity = 64)
    override val commandStream: SharedFlow<Command> = _commandStream.asSharedFlow()

    private val _commandResults = MutableSharedFlow<Pair<Command, CommandResult>>(extraBufferCapacity = 64)
    override val commandResults: SharedFlow<Pair<Command, CommandResult>> = _commandResults.asSharedFlow()

    override fun updateContext(context: OverlayContext) {
        _currentContext.value = context
    }

    override suspend fun dispatch(command: Command): CommandResult {
        _commandStream.emit(command)
        val context = currentContext.value

        if (!command.canExecute(context)) {
            val invalidResult = CommandResult.InvalidContext(
                "Command ${command::class.simpleName} cannot execute in current context"
            )
            Log.w(TAG, "Command execution rejected: $invalidResult")
            _commandResults.emit(command to invalidResult)
            return invalidResult
        }

        val result = try {
            command.execute()
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception during execution of ${command::class.simpleName}", e)
            CommandResult.Failure("Exception: ${e.message}")
        }

        Log.i(TAG, "Command ${command::class.simpleName} finished with result: $result")
        _commandResults.emit(command to result)
        return result
    }

    companion object {
        private const val TAG = "CommandMediator"
    }
}
