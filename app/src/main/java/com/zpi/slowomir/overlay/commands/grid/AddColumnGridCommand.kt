package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class AddColumnGridCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("num") private val num: Int = 1
    ) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("num" to num)


    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")

        gridExecutor.addCols(num)
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return gridExecutorProvider.get() != null
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("num") num: Int = 1
        ): AddColumnGridCommand
    }
}