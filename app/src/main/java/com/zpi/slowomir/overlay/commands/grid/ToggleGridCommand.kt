package com.zpi.slowomir.overlay.commands.grid

import android.util.Log
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class ToggleGridCommand @AssistedInject constructor(
    private val gridExcecutorProvider: Provider<GridExecutor?>
) : Command {

    override fun execute(): CommandResult {
        val gridExecutor = gridExcecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")

        gridExecutor.toggle()
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
        ): ToggleGridCommand
    }
}