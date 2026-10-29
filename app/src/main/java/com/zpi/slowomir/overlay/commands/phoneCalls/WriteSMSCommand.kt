package com.zpi.slowomir.overlay.commands.phoneCalls

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that opens or prepares an SMS message draft.
 *
 * @param phoneCallExecutorProvider A provider for the [PhoneCallExecutor].
 */
class WriteSMSCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = phoneCallExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.writeSMS()
        return if (success) CommandResult.Success() else CommandResult.Failure("Write SMS failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): WriteSMSCommand
    }
}