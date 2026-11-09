package com.zpi.slowomir.overlay.commands.touch

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_DISTANCE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeUpCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
    @Assisted("distance") private val distance: Float = DEFAULT_SWIPE_DISTANCE,
) : Command {

    override fun execute(): CommandResult {
        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.swipeUp(x, y, distance)
        return if (success) CommandResult.Success() else CommandResult.Failure("Swipe failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("x") x: Float,
            @Assisted("y") y: Float,
            @Assisted("distance") distance: Float = DEFAULT_SWIPE_DISTANCE,

            ): SwipeUpCommand
    }
}