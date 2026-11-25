package com.zpi.slowomir.overlay.commands.overlay

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * A command that puts the overlay to sleep: speech recognition stops and only keyword spotting stays on.
 *
 * It has nothing to execute itself. The voice session switches to keyword spotting when the command
 * finishes (see [returnsToKeywordSpotting]).
 */
class SleepCommand @AssistedInject constructor() : Command {
    override val returnsToKeywordSpotting: Boolean = true

    override fun execute(): CommandResult = CommandResult.Success()

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): SleepCommand
    }
}
