package com.zpi.slowomir.overlay.commands

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.executors.TouchExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that simulates a tap gesture at the specified screen coordinates.
 *
 * @param touchExecutorProvider A provider for the touch executor.
 * @param x The x-coordinate of the tap location.
 * @param y The y-coordinate of the tap location.
 */
class TapCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
) : Command {

    override fun execute(): CommandResult {
        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.tapAt(x, y)
        return if (success) CommandResult.Success() else CommandResult.Failure("Tap failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("x") x: Float,
            @Assisted("y") y: Float,
        ): TapCommand
    }
}