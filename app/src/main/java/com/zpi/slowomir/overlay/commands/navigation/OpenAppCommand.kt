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
 * A command that opens a specified application.
 *
 * @param navigationExecutorProvider A provider for the navigation executor.
 */
class OpenAppCommand @AssistedInject constructor(
    private val navigationExecutorProvider: Provider<NavigationExecutor?>,
    @Assisted("packageName") private val packageName: String?
) : Command {
    override fun execute(): CommandResult {
        if (packageName == null) {
            return CommandResult.Failure("No package name provided")
        }

        val executor = navigationExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.openApp(packageName)
        return if (success) CommandResult.Success() else CommandResult.Failure("Navigation open app failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("packageName") packageName: String?
        ): OpenAppCommand
    }
}