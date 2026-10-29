package com.zpi.slowomir.overlay.commands.predictions

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PredictionExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

class ConfirmCommand @AssistedInject constructor(
    private val predictionExecutorProvider: Provider<PredictionExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = predictionExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.confirm()
        return if (success) CommandResult.Success() else CommandResult.Failure("Predicted action confirmation failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): ConfirmCommand
    }
}