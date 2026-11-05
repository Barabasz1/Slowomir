package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeUpCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("distance") private val distance: Float = 500f
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        val success = gridExecutor.swipeUpCell(distance)
        return if (success) CommandResult.Success() else CommandResult.Failure("Swipe up on cell failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("distance") distance: Float = 500f
        ): SwipeUpCellCommand
    }
}
