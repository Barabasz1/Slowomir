package com.zpi.slowomir.overlay.commands.navigation

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that opens the list of all installed apps (app drawer / app library).
 *
 * @param navigationExecutorProvider A provider for the navigation executor.
 */
class NavigateAppLibraryCommand @AssistedInject constructor(
    private val navigationExecutorProvider: Provider<NavigationExecutor?>,
) : Command {

    override fun execute(): CommandResult {
        val executor = navigationExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.navigateAppLibrary()
        if (!success) return CommandResult.Failure("The launcher does not support opening the app library")

        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): NavigateAppLibraryCommand
    }
}
