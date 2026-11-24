package com.zpi.slowomir.overlay.executors

import android.util.Log
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.prediction.PredictionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlinx.coroutines.runBlocking

/**
 * Handles execution of predicted actions using Accessibility and Command Mediator.
 */
class PredictionExecutor(
    private val predictionManager: PredictionManager,
    private val tapCommandFactory: TapCommand.Factory,
    private val mediator: CommandMediator,
) {
    fun hasActiveSuggestion(): Boolean = predictionManager.getActiveSuggestion() != null

    /**
     * Performs a predicted action.
     */
    fun confirm(): Boolean {
        val bounds = runBlocking { predictionManager.onPredictionConfirmed() }
        if (bounds == null) {
            Log.w(TAG, "Prediction confirm double-check failed or no active suggestion")
            return false
        }

        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()

        val tapCommand = tapCommandFactory.create(x, y)
        val result = runBlocking { mediator.dispatch(tapCommand) }
        Log.i(TAG, "Prediction executed tap at ($x, $y), result: $result")
        return result is CommandResult.Success
    }

    /**
     * Declines a predicted action.
     */
    fun dismiss(): Boolean {
        return runBlocking { predictionManager.onPredictionDismissed() }
    }

    companion object {
        private const val TAG = "PredictionExecutor"
    }
}
