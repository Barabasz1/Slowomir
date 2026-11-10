package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_PINCH_POWER
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class ZoomInCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("power") private val power: Float = DEFAULT_PINCH_POWER,
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        val success = gridExecutor.pinchOutCell(power)
        return if (success) CommandResult.Success() else CommandResult.Failure("Zoom in on cell failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("power") power: Float = DEFAULT_PINCH_POWER,
        ): ZoomInCellCommand
    }
}
