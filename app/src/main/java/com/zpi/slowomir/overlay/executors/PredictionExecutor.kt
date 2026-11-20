package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.prediction.PredictionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlinx.coroutines.runBlocking

/**
 * Handles execution of predicted actions using Accessibility and Command Mediator.
 */
class PredictionExecutor(
    private val service: AccessibilityService,
    private val predictionManager: PredictionManager,
    private val tapCommandFactory: TapCommand.Factory,
    private val mediator: CommandMediator,
) {
    /**
     * Performs a predicted action.
     */
    fun confirm(): Boolean {
        val suggestion = predictionManager.getActiveSuggestion() ?: return false
        val confirmed = runBlocking { predictionManager.onPredictionConfirmed() }
        if (!confirmed) {
            Log.w(TAG, "Prediction confirm double-check failed")
            return false
        }

        val bounds = suggestion.bounds
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()

        val tapCommand = tapCommandFactory.create(x, y)
        val result = runBlocking { mediator.dispatch(tapCommand) }
        Log.i(TAG, "Prediction executed tap at ($x, $y), result: $result")
        return true
    }

    /**
     * Declines a predicted action.
     */
    fun dismiss(): Boolean {
        runBlocking { predictionManager.onPredictionDismissed() }
        return true
    }

    companion object {
        private const val TAG = "PredictionExecutor"
    }
}
