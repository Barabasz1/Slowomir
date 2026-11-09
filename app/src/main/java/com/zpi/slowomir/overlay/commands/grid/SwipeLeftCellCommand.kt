package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_DISTANCE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeLeftCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("distance") private val distance: Float = DEFAULT_SWIPE_DISTANCE
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        val success = gridExecutor.swipeLeftCell(distance)
        return if (success) CommandResult.Success() else CommandResult.Failure("Swipe up left cell failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("distance") distance: Float = DEFAULT_SWIPE_DISTANCE
        ): SwipeLeftCellCommand
    }
}
