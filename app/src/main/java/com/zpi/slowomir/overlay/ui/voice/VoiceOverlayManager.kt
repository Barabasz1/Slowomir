package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.VoiceOverlayView
import com.zpi.slowomir.overlay.ui.common.OverlayHost
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.voice.mediator.CommandMediator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI Window Manager that renders the floating [com.zpi.slowomir.overlay.ui.VoiceOverlayView] on top of system interfaces.
 * Displays live speech-to-text transcriptions and pipeline execution states in real-time.
 */
@Singleton
class VoiceOverlayManager @Inject constructor(
    private val commandMediator: CommandMediator,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {

    override val tag = "VoiceOverlayManager"
    private var lifecycleOwner: OverlayLifecycleOwner? = null

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
                SłowomirTheme {
                    VoiceOverlayView(mediator = commandMediator)
                }
            }
        }
    }

    override fun onCreateLayoutParams(windowType: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    override fun onViewDetached(view: View) {
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    override fun onAttachDenied(host: OverlayHost) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${host.context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { host.context.startActivity(intent) }
            .onFailure { Log.e(tag, "Failed to open overlay permission screen", it) }
    }

    suspend fun showOverlay(): Boolean = withContext(Dispatchers.Main.immediate) { show() }
    suspend fun hideOverlay() = withContext(Dispatchers.Main.immediate) { hide() }
    suspend fun toggleOverlay(): Boolean = withContext(Dispatchers.Main.immediate) {
        if (isShowing) {
            hide(); true
        } else show()
    }
}