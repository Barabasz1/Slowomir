package com.zpi.slowomir.overlay.commands.navigation

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that swipes right from the specified screen coordinates.
 *
 * @param navigationExecutorProvider A provider for the navigation executor.
 * @param x The x-coordinate of the swipe start location.
 * @param y The y-coordinate of the swipe start location.
 */
class SwipeRightCommand @AssistedInject constructor(
    private val navigationExecutorProvider: Provider<NavigationExecutor?>,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
) : Command {
    override fun execute(): CommandResult {
        val executor = navigationExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.swipeRight(x, y)
        return if (success) CommandResult.Success() else CommandResult.Failure("Navigation swipe right failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("x") x: Float,
            @Assisted("y") y: Float,
        ): SwipeRightCommand
    }
}
