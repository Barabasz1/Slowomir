package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class EnableGridCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")

        gridExecutor.enable()
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return !(gridExecutorProvider.get()?.isEnabled() ?: true)
    }

    @AssistedFactory
    interface Factory {
        fun create(
        ): EnableGridCommand
    }
}