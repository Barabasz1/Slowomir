package com.zpi.slowomir.overlay.commands.phoneCalls

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that ends an ongoing phone call.
 *
 * @param phoneCallExecutorProvider A provider for the [PhoneCallExecutor].
 */
class EndCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = phoneCallExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.end()
        return if (success) CommandResult.Success() else CommandResult.Failure("Phone call end failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): EndCommand
    }
}