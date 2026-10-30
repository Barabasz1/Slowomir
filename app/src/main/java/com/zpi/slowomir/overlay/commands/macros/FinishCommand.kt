package com.zpi.slowomir.overlay.commands.macros

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.MacroExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that finishes recording a new macro.
 *
 * @param macroExecutorProvider A provider for the macro executor.
 */
class FinishCommand @AssistedInject constructor(
    private val macroExecutorProvider: Provider<MacroExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = macroExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.finish()
        return if (success) CommandResult.Success() else CommandResult.Failure("Macro record finish failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): FinishCommand
    }
}