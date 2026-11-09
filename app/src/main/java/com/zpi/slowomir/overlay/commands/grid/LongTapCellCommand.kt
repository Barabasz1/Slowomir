package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_HOLD_DURATION
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class LongTapCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("duration") private val duration: Long = DEFAULT_HOLD_DURATION,
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        gridExecutor.longTapCell(duration)
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("duration") duration: Long = DEFAULT_HOLD_DURATION
        ): LongTapCellCommand
    }
}