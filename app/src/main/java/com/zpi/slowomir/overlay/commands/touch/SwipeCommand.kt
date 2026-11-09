package com.zpi.slowomir.overlay.commands.touch

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TouchExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    @Assisted("startX") private val startX: Float,
    @Assisted("startY") private val startY: Float,
    @Assisted("destX") private val destX: Float,
    @Assisted("destY") private val destY: Float,
) : Command {

    override fun execute(): CommandResult {
        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.swipe(startX, startY, destX, destY)
        return if (success) CommandResult.Success() else CommandResult.Failure("Swipe failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("startX") startX: Float,
            @Assisted("startY") startY: Float,
            @Assisted("destX") destX: Float,
            @Assisted("destY") destY: Float,
        ): SwipeCommand
    }
}