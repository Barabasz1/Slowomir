package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_PINCH_POWER
import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class ZoomOutCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("power") private val power: Float = DEFAULT_PINCH_POWER,
) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("power" to power)


    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        val success = gridExecutor.pinchInCell(power)
        return if (success) CommandResult.Success() else CommandResult.Failure("Zoom in on cell failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return CommandExecutorService.instance != null && (gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("power") power: Float = DEFAULT_PINCH_POWER,
        ): ZoomOutCellCommand
    }
}
