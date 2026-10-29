package com.zpi.slowomir.overlay.commands.media

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.MediaExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class PauseCommand @AssistedInject constructor(
    private val mediaExecutorProvider: Provider<MediaExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = mediaExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.pause()
        return if (success) CommandResult.Success() else CommandResult.Failure("Media pause failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): PauseCommand
    }
}