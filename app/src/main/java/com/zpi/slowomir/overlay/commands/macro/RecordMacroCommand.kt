package com.zpi.slowomir.overlay.commands.macro

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.MacroExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that records a new macro.
 *
 * @param macroExecutorProvider A provider for the macro executor.
 * @param here Remember the current app and screen as the starting point of the macro.
 */
class RecordMacroCommand @AssistedInject constructor(
    private val macroExecutorProvider: Provider<MacroExecutor?>,
    @Assisted("here") private val here: Boolean = false,
) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("here" to here)

    override fun execute(): CommandResult {
        val executor = macroExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.record(here)
        return if (success) CommandResult.Success() else CommandResult.Failure("Macro record failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("here") here: Boolean = false): RecordMacroCommand
    }
}