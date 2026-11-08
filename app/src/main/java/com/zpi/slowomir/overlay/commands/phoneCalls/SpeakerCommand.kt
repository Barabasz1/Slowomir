package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Provider

/**
 * A command that toggles speakerphone mode for a call.
 *
 * @param phoneCallExecutorProvider A provider for the [PhoneCallExecutor].
 * @param context The application context to fall back to AudioManager if needed.
 */
class SpeakerCommand @AssistedInject constructor(
    private val phoneCallExecutorProvider: Provider<PhoneCallExecutor?>,
    @ApplicationContext private val context: Context,
) : Command {
    override fun execute(): CommandResult {
        val executor = phoneCallExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        return if (executor.speakerToggle()) {
            CommandResult.Success()
        }
        else {
            CommandResult.Failure("Could not toggle speakerphone")
        }
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): SpeakerCommand
    }
}
