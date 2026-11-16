package com.zpi.slowomir.overlay.commands.navigation

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.NavigationExecutor
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionLoggable
import com.zpi.slowomir.overlay.log.ActionType
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that navigates back.
 * A successful execution is stored in the action log as `BACK`.
 *
 * @param navigationExecutorProvider A provider for the navigation executor.
 */
class NavigateBackCommand @AssistedInject constructor(
    private val navigationExecutorProvider: Provider<NavigationExecutor?>,
) : Command, ActionLoggable {

    private var loggedAction: ActionDescriptor? = null

    override fun execute(): CommandResult {
        loggedAction = null

        val executor = navigationExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.navigateBack()
        if (!success) return CommandResult.Failure("Navigate back failed")

        loggedAction = ActionDescriptor.system(ActionType.BACK)
        return CommandResult.Success()
    }

    override fun consumeLoggedAction(): ActionDescriptor? {
        return loggedAction.also { loggedAction = null }
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): NavigateBackCommand
    }
}