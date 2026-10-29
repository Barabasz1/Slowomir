package com.zpi.slowomir.overlay.commands.macros

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that swipes down.
 *
 * @param navigationExecutorProvider A provider for the navigation executor.
 */
class SwipeDownCommand @AssistedInject constructor(
    private val navigationExecutorProvider: Provider<NavigationExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = navigationExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.swipeDown()
        return if (success) CommandResult.Success() else CommandResult.Failure("Navigation swipe down failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): SwipeDownCommand
    }
}