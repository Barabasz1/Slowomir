package com.zpi.slowomir.overlay.commands.touch

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_HOLD_DURATION
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class LongTapCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
    @Assisted("duration") private val duration: Long = DEFAULT_HOLD_DURATION
) : Command {

    override fun execute(): CommandResult {
        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.longTap(x, y, duration)
        return if (success) CommandResult.Success() else CommandResult.Failure("Long tap failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("x") x: Float,
            @Assisted("y") y: Float,
            @Assisted("duration") duration: Long = DEFAULT_HOLD_DURATION
        ): LongTapCommand
    }
}