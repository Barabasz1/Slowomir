package com.zpi.slowomir.overlay.commands.media

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.MediaExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that skips media playback forward.
 *
 * @param mediaExecutorProvider A provider for the [MediaExecutor].
 * @param seconds How many seconds to skip; `null` uses the default step.
 */
class SkipForwardCommand @AssistedInject constructor(
    private val mediaExecutorProvider: Provider<MediaExecutor?>,
    @Assisted("seconds") private val seconds: Int? = null,
) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("seconds" to seconds)

    override fun execute(): CommandResult {
        val executor = mediaExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.skipForward(seconds)
        return if (success) CommandResult.Success() else CommandResult.Failure("Media skip forward failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("seconds") seconds: Int? = null): SkipForwardCommand
    }
}
