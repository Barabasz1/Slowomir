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
 * A command that plays a saved macro.
 *
 * @param macroExecutorProvider A provider for the macro executor.
 */
class PlayMacroCommand @AssistedInject constructor(
    private val macroExecutorProvider: Provider<MacroExecutor?>,
    @Assisted("index") private val index: Int
) : Command {
    override fun execute(): CommandResult {
        val executor = macroExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.playFromList(index)
        return if (success) CommandResult.Success() else CommandResult.Failure("Macro play failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("index") index: Int
        ): PlayMacroCommand
    }
}