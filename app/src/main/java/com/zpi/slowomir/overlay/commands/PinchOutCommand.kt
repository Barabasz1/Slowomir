package com.zpi.slowomir.overlay.commands

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.touch.TouchExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class PinchOutCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
    @Assisted("power") private val power: Float = 1f,
) : Command {

    override fun execute(): CommandResult {
        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.pinchOut(x, y, power)
        return if (success) CommandResult.Success() else CommandResult.Failure("Pinch out failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("x") x: Float,
            @Assisted("y") y: Float,
            @Assisted("power") power: Float = 1f,
        ): PinchOutCommand
    }
}
