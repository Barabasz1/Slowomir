package com.zpi.slowomir.overlay.commands

import android.util.Log
import com.zpi.slowomir.overlay.OverlayContext
import dagger.assisted.AssistedFactory

/**
 * Stub command that logs intent details without performing real actions.
 * Used as a placeholder for commands whose executors are not yet implemented.
 *
 * @param identifier The [CommandIdentifier] describing what action was requested.
 * @param argument The optional [CommandArgument] extracted from voice input.
 */
class LogCommand(
    private val identifier: CommandIdentifier,
    private val argument: CommandArgument?
) : Command {

    override fun execute(): CommandResult {
        val argDescription = when (argument) {
            is CommandArgument.Number -> "Number(${argument.value})"
            is CommandArgument.TwoNumbers -> "TwoNumbers(from=${argument.from}, to=${argument.to})"
            is CommandArgument.Text -> "Text(\"${argument.value}\")"
            null -> "none"
        }
        Log.i(TAG, "Command dispatched: $identifier | argument: $argDescription")
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean = true

    companion object {
        private const val TAG = "LogCommand"
    }

    @AssistedFactory
    interface Factory {
        fun create(
        ): LogCommand
    }
}

