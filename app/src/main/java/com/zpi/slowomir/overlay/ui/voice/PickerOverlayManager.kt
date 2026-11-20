package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.overlay.ui.macro.MacroEntry
import com.zpi.slowomir.overlay.ui.macro.MacroView
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.selection.Selection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PickerOverlayManager @Inject constructor(
    private val voiceSessionManager: VoiceSessionManager,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {

    override val tag = "SelectionPickerOverlay"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private data class PickerState(
        val selection: Selection? = null,
    )

    private val _pickerState = MutableStateFlow(PickerState())

    init {
        scope.launch {
            voiceSessionManager.selection.collect(::onSelectionChanged)
        }
    }

    private fun onSelectionChanged(selection: Selection?) {
        hide()
        if (selection == null || selection.size == 0) {
            _pickerState.value = PickerState(null)
        } else {
            _pickerState.value = PickerState(selection)
            if (!show()) {
                voiceSessionManager.cancelSelection()
            }
        }
    }

    override fun onCreateView(context: Context): View {
        val lifecycle = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        lifecycleOwner = lifecycle

        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)

            setContent {
                val state by _pickerState.collectAsState()
                val selection = state.selection
                SłowomirTheme {
                    when (selection) {
                        is Selection.MacroSelection -> {
                            val entries = selection.options.map { MacroEntry(it) }
                            MacroView(
                                macroList = entries,
                                onSelectMacro = { index -> voiceSessionManager.selectOption(index) },
                                onClose = { voiceSessionManager.cancelSelection() },
                            )
                        }

                        is Selection.Contact -> {
                            val options = selection.options.map { PickerOption(it.name) }
                            SelectionPickerView(
                                options = options,
                                action = selection.pending.identifier,
                                onSelect = { index -> voiceSessionManager.selectOption(index) },
                                onCancel = { voiceSessionManager.cancelSelection() },
                            )
                        }

                        is Selection.Application -> {
                            val options = selection.options.map { PickerOption(it.name, it.icon) }
                            SelectionPickerView(
                                options = options,
                                action = selection.pending.identifier,
                                onSelect = { index -> voiceSessionManager.selectOption(index) },
                                onCancel = { voiceSessionManager.cancelSelection() },
                            )
                        }

                        null -> Unit
                    }
                }
            }
        }
    }

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams {
        val selection = _pickerState.value.selection
        val width = (Resources.getSystem().displayMetrics.widthPixels * WIDTH_FRACTION).toInt()
        val height = if (selection is Selection.MacroSelection) {
            (Resources.getSystem().displayMetrics.heightPixels * MacroView.HEIGHT_FRACTION).toInt()
        } else {
            WindowManager.LayoutParams.WRAP_CONTENT
        }

        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    override fun onViewDetached(view: View) {
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    companion object {
        private const val WIDTH_FRACTION = 0.9f
    }
}
