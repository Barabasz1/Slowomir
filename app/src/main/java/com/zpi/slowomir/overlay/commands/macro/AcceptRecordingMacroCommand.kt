package com.zpi.slowomir.overlay.commands.macro

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.MacroExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class AcceptRecordingMacroCommand @AssistedInject constructor(
    private val macroExecutorProvider: Provider<MacroExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = macroExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.acceptRecording()
        return if (success) CommandResult.Success() else CommandResult.Failure("Macro accept recording failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return !(macroExecutorProvider.get()?.isRecordingEmpty() ?: true)
    }

    @AssistedFactory
    interface Factory {
        fun create(): AcceptRecordingMacroCommand
    }
}