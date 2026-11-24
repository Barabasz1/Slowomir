package com.zpi.slowomir.overlay.ui.confirmation

import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlaySide
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.overlay.ui.voice.VoiceOverlayManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmationOverlayManager @Inject constructor(
    private val confirmationManager: ConfirmationManager,
    private val voiceOverlayManager: VoiceOverlayManager,
    hostProvider: OverlayHostProvider,
) : OverlayWindowManager(hostProvider) {

    override val tag = "ConfirmationOverlay"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    init {
        scope.launch {
            confirmationManager.request.collect { request ->
                if (request == null) hide() else if (!isShowing) show()
            }
        }
        scope.launch {
            voiceOverlayManager.strip.collect { strip ->
                updateLayout {
                    gravity = strip.side.toGravity()
                    y = strip.sizePx
                }
            }
        }
    }

    private fun OverlaySide.toGravity() =
        (if (this == OverlaySide.TOP) Gravity.TOP else Gravity.BOTTOM) or Gravity.CENTER_HORIZONTAL

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
                val request by confirmationManager.request.collectAsState()
                val density = context.resources.displayMetrics.density
                val maxContentHeight = (context.resources.displayMetrics.heightPixels * CONTENT_HEIGHT_FRACTION / density).dp
                SłowomirTheme {
                    request?.let {
                        ConfirmationView(
                            request = it,
                            maxContentHeight = maxContentHeight,
                            onAnswer = { yes -> confirmationManager.answer(yes) },
                        )
                    }
                }
            }
        }
    }

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams {
        val strip = voiceOverlayManager.strip.value
        val width = (Resources.getSystem().displayMetrics.widthPixels * WIDTH_FRACTION).toInt()
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = strip.side.toGravity()
            y = strip.sizePx
        }
    }

    override fun onViewDetached(view: View) {
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    private companion object {
        const val WIDTH_FRACTION = 0.94f
        const val CONTENT_HEIGHT_FRACTION = 0.5f
    }
}
