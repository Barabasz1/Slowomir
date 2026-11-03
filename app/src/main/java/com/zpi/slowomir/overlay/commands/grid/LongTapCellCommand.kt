package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class LongTapCellCommand @AssistedInject constructor(
    private val gridExcecutorProvider: Provider<GridExecutor?>,
    @Assisted("cell") private val cell: Int,
    @Assisted("duration") private val duration: Long,
    ) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExcecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        gridExecutor.longTapCell(cell, duration)
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("cell") cell: Int,
            @Assisted("duration") duration: Long
        ): LongTapCellCommand
    }
}