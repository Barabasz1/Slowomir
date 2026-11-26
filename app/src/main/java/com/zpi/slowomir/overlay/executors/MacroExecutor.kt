package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.macro.MacroOverlay
import com.zpi.slowomir.overlay.macro.MacroPlayer
import com.zpi.slowomir.overlay.macro.MacroRecorder
import com.zpi.slowomir.overlay.macro.MacroRepository
import com.zpi.slowomir.overlay.macro.MacroScreenState
import com.zpi.slowomir.overlay.macro.MacroSupplier
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroExecutor @Inject constructor(
    private val macroPlayer: MacroPlayer,
    private val macroSupplier: MacroSupplier,
    private val macroRecorder: MacroRecorder,
    private val macroOverlay: MacroOverlay,
    private val macroRepository: MacroRepository,
    private val macroScreenState: MacroScreenState,
) {
    fun record(here: Boolean = false): Boolean {
        return macroRecorder.startRecording(here)
    }

    fun finish(): Boolean {
        return macroRecorder.finishRecording()
    }

    fun discardRecording(): Boolean {
        return macroRecorder.discardRecording()
    }

    fun acceptRecording(): Boolean {
        return macroRecorder.acceptRecording()
    }

    fun playFromList(index: Int): Boolean {
        return macroPlayer.play(macroSupplier.getMacroFromList(index))
    }

    fun stopMacro(): Boolean {
        macroPlayer.stop()
        return true
    }

    fun showMacroOverlay(): Boolean {
        return macroOverlay.showOverlay()
    }

    fun hideMacroOverlay(): Boolean {
        return macroOverlay.hideOverlay()
    }

    fun toggleMacroOverlay(): Boolean {
        return macroOverlay.toggle()
    }

    fun isOverlayShown(): Boolean {
        return macroOverlay.isShowing
    }

    fun hasMacroOption(index: Int): Boolean {
        return macroOverlay.hasOption(index)
    }

    fun deleteLastRecordedAction(): Boolean {
        return macroRecorder.deleteLastAction()
    }

    fun canDeleteLastRecordedAction(): Boolean {
        return macroRecorder.hasRecordedActions()
    }

    fun isRecording(): Boolean {
        return macroRecorder.isRecording()
    }

    fun isAwaitingRecordingConfirmation(): Boolean {
        return macroRecorder.isAwaitingConfirmation()
    }


    fun canDeleteRecordedMacro(index: Int): Boolean {
        return macroScreenState.isVisible.value && index in macroRepository.recordedMacros.value.indices
    }

    fun canKeepGeneratedMacro(index: Int): Boolean {
        val generatedIndex = index - macroRepository.recordedMacros.value.size
        return macroScreenState.isVisible.value && generatedIndex in macroRepository.generatedMacros.value.indices
    }

    fun keepGeneratedMacro(index: Int): Boolean {
        val kept = macroRepository.keepGeneratedMacro(index - macroRepository.recordedMacros.value.size)
        if (kept) macroSupplier.refresh()
        return kept
    }

    fun deleteRecordedMacro(index: Int): Boolean {
        val deleted = macroRepository.deleteRecordedMacro(index)
        if (deleted) {
            macroSupplier.refresh()
            runBlocking { macroRepository.reloadGeneratedMacros() }
        }
        return deleted
    }
}
