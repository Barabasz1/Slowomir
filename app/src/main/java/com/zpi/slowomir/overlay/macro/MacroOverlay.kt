package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.ui.voice.PickerOverlayManager
import com.zpi.slowomir.voice.VoiceIntent
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.selection.Selection
import com.zpi.slowomir.voice.selection.SelectionManager
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton


// Wrapper, there is no overlay for macros, only the selection picker
@Singleton
class MacroOverlay @Inject constructor(
    private val selectionManager: SelectionManager,
    private val voiceSessionManager: VoiceSessionManager,
    private val pickerOverlayManager: PickerOverlayManager,
    private val macroSupplier: MacroSupplier,
) {
    val isShowing: Boolean
        get() = pickerOverlayManager.isShowing && selectionManager.selection.value is Selection.Macro

    fun showOverlay(): Boolean {
        runBlocking { selectionManager.begin(VoiceIntent(CommandIdentifier.MACRO_SHOW, null)) }
        return pickerOverlayManager.isShowing
    }

    fun hideOverlay(): Boolean {
        if (selectionManager.selection.value is Selection.Macro) {
            selectionManager.cancel()
        }
        return true
    }

    fun toggle(): Boolean {
        return if (isShowing) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    fun selectMacro(position: Int) {
        voiceSessionManager.selectOption(position)
    }

    fun hasOption(index: Int): Boolean {
        // selection immediately clears options after user says number, so fallback to macro list
        val options = (selectionManager.selection.value as? Selection.Macro)?.options ?: macroSupplier.getMacroList()
        return index in options.indices
    }
}
