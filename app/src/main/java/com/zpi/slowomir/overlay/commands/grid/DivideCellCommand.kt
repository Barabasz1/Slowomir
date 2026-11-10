package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class DivideCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("divisionPower") private val divisionPower: Int = 2
    ) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")

        gridExecutor.divide(divisionPower)
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("divisionPower") divisionPower: Int = 2
        ): DivideCellCommand
    }
}