package com.zpi.slowomir.overlay.commands.navigation

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import com.zpi.slowomir.utils.SwipeDirection
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_FORCE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeLeftNavCommand @AssistedInject constructor(
    private val navigationExecutorProvider: Provider<NavigationExecutor?>,
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("force") private val force: Int = DEFAULT_SWIPE_FORCE,
) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("force" to force)

    override fun execute(): CommandResult {
        val navExecutor = navigationExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")
        val gridExecutor = gridExecutorProvider.get()

        val success = if (gridExecutor != null && gridExecutor.canExecuteActionOnCell())
            gridExecutor.swipeCell(SwipeDirection.LEFT, force)
        else
            navExecutor.swipe(SwipeDirection.LEFT, force)

        return if (success) CommandResult.Success() else CommandResult.Failure("Navigation swipe left failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("force") force: Int = DEFAULT_SWIPE_FORCE
        ): SwipeLeftNavCommand
    }
}
