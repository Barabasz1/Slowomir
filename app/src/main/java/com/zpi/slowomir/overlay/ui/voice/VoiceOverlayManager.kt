package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.VoiceOverlayView
import com.zpi.slowomir.overlay.ui.lifecycle.OverlayLifecycleOwner
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
    @ApplicationContext private val context: Context
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isOverlayShowing = false

    /**
     * Shows the floating overlay window bound to [CommandMediator] UI state.
     *
     * @return True if the overlay window was added successfully.
     */
    suspend fun showOverlay(): Boolean = withContext(Dispatchers.Main.immediate) {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot draw overlay: SYSTEM_ALERT_WINDOW permission not granted.")
            requestOverlayPermission()
            return@withContext false
        }

        if (overlayView != null) {
            isOverlayShowing = true
            return@withContext true
        }

        val lifecycle = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
        }
        lifecycleOwner = lifecycle

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)

            setContent {
                SłowomirTheme {
                    VoiceOverlayView(mediator = commandMediator)
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        return@withContext try {
            windowManager.addView(view, params)
            overlayView = view
            isOverlayShowing = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add system overlay view", e)
            lifecycle.onDestroy()
            lifecycleOwner = null
            isOverlayShowing = false
            false
        }
    }

    suspend fun hideOverlay() = withContext(Dispatchers.Main.immediate) {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
        }
        overlayView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        isOverlayShowing = false
    }

    suspend fun toggleOverlay(): Boolean = withContext(Dispatchers.Main.immediate) {
        return@withContext if (!isOverlayShowing) {
            showOverlay()
        } else {
            hideOverlay()
            true
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay permission activity", e)
        }
    }

    companion object {
        private const val TAG = "VoiceOverlayManager"
    }
}