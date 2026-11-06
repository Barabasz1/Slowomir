package com.zpi.slowomir.overlay.commands.launching

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.AppLaunchExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


/**
 * A command that launches a specified application.
 *
 * @param appLaunchExecutorProvider A provider for the app launch executor.
 */
class LaunchAppCommand  @AssistedInject constructor(
    private val appLaunchExecutorProvider: Provider<AppLaunchExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = appLaunchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.navigateBack()
        return if (success) CommandResult.Success() else CommandResult.Failure("Failed to launch app")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): LaunchAppCommand
    }
}
