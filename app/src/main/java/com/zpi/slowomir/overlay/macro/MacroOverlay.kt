package com.zpi.slowomir.overlay.macro

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroOverlay @Inject constructor(
    hostProvider: OverlayHostProvider,
    private val macroSupplier: MacroSupplier,
    private val macroPlayer: MacroPlayer,
) : OverlayWindowManager(hostProvider) {

    override val tag = "MacroOverlay"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private data class MacroOverlayState(
        val entries: List<MacroEntry> = emptyList()
    )

    private val _macroState = MutableStateFlow(MacroOverlayState())

    init {
    }
    fun showOverlay(): Boolean {
        hide()
        val macros = macroSupplier.getMacroList()
        val entries = macros.map { macro ->
            MacroEntry(macro)
        }
        _macroState.value = MacroOverlayState(entries = entries)
        return show()
    }

    fun hideOverlay(): Boolean {
        hide()
        return true
    }


    override fun toggle(): Boolean {
        return if (isShowing) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun redraw() {
        if (isShowing) {
            val macros = macroSupplier.getMacroList()
            val entries = macros.map { macro ->
                MacroEntry(macro)
            }
            _macroState.value = MacroOverlayState(entries = entries)
        }
    }

    fun selectMacro(position: Int) {
        val macroIndex = position - 1
        val macros = macroSupplier.getMacroList()
        hide()
        if (macroIndex in macros.indices) {
            val macro = macros[macroIndex]
            scope.launch(Dispatchers.Default) {
                macroPlayer.play(macro.toMacro())
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
                val state by _macroState.collectAsState()
                SłowomirTheme {
                    MacroView(
                        macroList = state.entries,
                        onSelectMacro = { index -> selectMacro(index) },
                        onClose = { hide() },
                    )
                }
            }
        }
    }

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams {
        val width = (Resources.getSystem().displayMetrics.widthPixels * MacroView.WIDTH_FRACTION).toInt()
        val height = (Resources.getSystem().displayMetrics.heightPixels * MacroView.HEIGHT_FRACTION).toInt()
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
}
