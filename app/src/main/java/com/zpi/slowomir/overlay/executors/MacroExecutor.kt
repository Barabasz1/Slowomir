package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.macro.MacroOverlay
import com.zpi.slowomir.overlay.macro.MacroPlayer
import com.zpi.slowomir.overlay.macro.MacroRecorder
import com.zpi.slowomir.overlay.macro.MacroSupplier
import com.zpi.slowomir.overlay.macro.toMacro
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroExecutor @Inject constructor(
    private val macroPlayer: MacroPlayer,
    private val macroSupplier: MacroSupplier,
    private val macroRecorder: MacroRecorder,
    private val macroOverlay: MacroOverlay
) {
    fun record(): Boolean {
        return macroRecorder.startRecording()
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
        return macroPlayer.play(macroSupplier.getMacroFromList(index).toMacro())
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
    fun isRecordingEmpty(): Boolean {
        return macroRecorder.isRecordingEmpty()
    }
}
