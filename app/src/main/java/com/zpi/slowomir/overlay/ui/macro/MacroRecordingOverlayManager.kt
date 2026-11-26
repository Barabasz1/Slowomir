package com.zpi.slowomir.overlay.ui.macro

import android.content.Context
import com.zpi.slowomir.overlay.macro.MacroRecorder
import com.zpi.slowomir.overlay.macro.RecordingState
import com.zpi.slowomir.overlay.ui.confirmation.ConfirmationManager
import com.zpi.slowomir.overlay.ui.confirmation.ConfirmationRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroRecordingOverlayManager @Inject constructor(
    private val macroRecorder: MacroRecorder,
    private val confirmationManager: ConfirmationManager,
    @ApplicationContext private val appContext: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var question: ConfirmationRequest? = null

    init {
        scope.launch {
            macroRecorder.state.collect(::onStateChanged)
        }
    }

    private fun onStateChanged(state: RecordingState) {
        when (state) {
            is RecordingState.Recording -> Unit

            is RecordingState.AwaitingConfirmation -> {
                askToSave(state)
            }

            RecordingState.Idle -> {
                question?.let { confirmationManager.dismiss(it) }
                question = null
            }
        }
    }

    private fun askToSave(state: RecordingState.AwaitingConfirmation) {
        val appLabel = appLabelResolver(appContext)
        val start = MacroCommandFormatter.formatStartingPoint(state.startingPoint, appLabel)
        val actions = (listOfNotNull(start) + state.commands.map { MacroCommandFormatter.format(it, appLabel) })
            .map { ActionEntry(it) }
        val request = ConfirmationRequest(
            title = "Zapisać makro?",
            onYes = { macroRecorder.acceptRecording() },
            onNo = { macroRecorder.discardRecording() },
            content = { MacroRecordingSummary(actions, actionCount = state.commands.size) },
        )
        question = request
        confirmationManager.ask(request)
    }
}
