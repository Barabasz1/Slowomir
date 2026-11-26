package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.utils.SwipeDirection
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_FORCE
import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeDownCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("force") private val force: Int = DEFAULT_SWIPE_FORCE,
) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("force" to force)

    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        val success = gridExecutor.swipeCell(SwipeDirection.DOWN, force)
        return if (success) CommandResult.Success() else CommandResult.Failure("Swipe down on cell failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return CommandExecutorService.instance != null && (gridExecutorProvider.get()
            ?.canExecuteActionOnCell() ?: false)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("force") force: Int = DEFAULT_SWIPE_FORCE
        ): SwipeDownCellCommand
    }
}
