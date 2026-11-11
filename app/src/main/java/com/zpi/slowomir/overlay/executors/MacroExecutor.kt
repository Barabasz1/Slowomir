package com.zpi.slowomir.overlay.executors

import android.text.BoringLayout
import com.zpi.slowomir.overlay.macro.MacroPlayer
import com.zpi.slowomir.overlay.macro.MacroRecorder
import com.zpi.slowomir.overlay.macro.MacroSupplier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroExecutor @Inject constructor(
    private val macroPlayer: MacroPlayer,
    private val macroSupplier: MacroSupplier,
    private val macroRecorder: MacroRecorder,
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
        return macroPlayer.play(macroSupplier.getMacroFromList(index))
    }

    fun stopMacro(): Boolean {
        macroPlayer.stop()
        return true
    }

    fun isRecordingEmpty(): Boolean {
        return macroRecorder.isRecordingEmpty()
    }
}
