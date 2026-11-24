package com.zpi.slowomir.overlay.commands.macro

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.MacroExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class KeepMacroCommand @AssistedInject constructor(
    private val macroExecutorProvider: Provider<MacroExecutor?>,
    @Assisted("index") private val index: Int,
) : Command {

    override val requiresBackgroundExecution: Boolean = true

    override val arguments: Map<String, Any?>
        get() = mapOf("index" to index)

    override fun execute(): CommandResult {
        val executor = macroExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.keepGeneratedMacro(index)
        return if (success) CommandResult.Success() else CommandResult.Failure("Keep macro failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return macroExecutorProvider.get()?.canKeepGeneratedMacro(index) ?: false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("index") index: Int
        ): KeepMacroCommand
    }
}
