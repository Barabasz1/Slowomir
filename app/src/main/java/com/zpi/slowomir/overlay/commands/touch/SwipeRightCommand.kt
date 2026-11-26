package com.zpi.slowomir.overlay.commands.touch

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.utils.SwipeDirection
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_FORCE
import com.zpi.slowomir.utils.ScreenPoint
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class SwipeRightCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
    @Assisted("force") private val force: Int = DEFAULT_SWIPE_FORCE,
) : Command {
    override val arguments: Map<String, Any?>
        get() = mapOf("x" to x, "y" to y, "force" to force)

    override fun execute(): CommandResult {
        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.swipe(ScreenPoint(x, y), SwipeDirection.RIGHT, force)
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
            @Assisted("force") force: Int = DEFAULT_SWIPE_FORCE,
        ): SwipeRightCommand
    }
}
