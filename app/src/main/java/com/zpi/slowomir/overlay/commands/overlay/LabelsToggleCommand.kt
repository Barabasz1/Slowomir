package com.zpi.slowomir.overlay.commands.overlay

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.LabelsExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/** Toggles the overlay that numbers the interactive fragments of the screen. */
class LabelsToggleCommand @AssistedInject constructor(
    private val labelsExecutorProvider: Provider<LabelsExecutor?>
) : Command {

    override fun execute(): CommandResult {
        val executor = labelsExecutorProvider.get()
            ?: return CommandResult.Failure("Labels executor is null")
        executor.toggle()
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean = true

    @AssistedFactory
    interface Factory {
        fun create(): LabelsToggleCommand
    }
}
