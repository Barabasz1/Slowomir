package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class TapCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        gridExecutor.tapCell()
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return CommandExecutorService.instance != null && (gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false)
    }

    @AssistedFactory
    interface Factory {
        fun create(
        ): TapCellCommand
    }
}