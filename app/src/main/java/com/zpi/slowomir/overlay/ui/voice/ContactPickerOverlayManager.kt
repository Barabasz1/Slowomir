package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.ContactPickerView
import com.zpi.slowomir.overlay.ui.lifecycle.OverlayLifecycleOwner
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactPickerOverlayManager @Inject constructor(
    private val mediator: CommandMediator,
    private val voiceSessionManager: VoiceSessionManager,
    @ApplicationContext private val context: Context,
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var observeJob: Job? = null
    private var timeoutJob: Job? = null
    private var view: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            mediator.mediatorState
                .map { it.contactSelection }
                .distinctUntilChanged()
                .collect { options ->
                    if (options.isNullOrEmpty()) hide() else show(options)
                }
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
        hide()
    }

    private fun show(options: List<ContactOption>) {
        hide()

        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show contact picker: overlay permission not granted.")
            voiceSessionManager.cancelSelection()
            return
        }

        val lifecycle = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)

            setContent {
                SłowomirTheme {
                    ContactPickerView(
                        options = options,
                        onSelect = { index -> voiceSessionManager.selectContact(index) },
                        onCancel = { voiceSessionManager.cancelSelection() },
                    )
                }
            }
        }

        val width = (context.resources.displayMetrics.widthPixels * WIDTH_FRACTION).toInt()
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(composeView, params)
            view = composeView
            lifecycleOwner = lifecycle
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add contact picker overlay", e)
            lifecycle.onDestroy()
            voiceSessionManager.cancelSelection()
            return
        }

        timeoutJob = scope.launch {
            delay(TIMEOUT_MS)
            Log.i(TAG, "Contact selection timed out")
            voiceSessionManager.cancelSelection()
        }
    }

    private fun hide() {
        timeoutJob?.cancel()
        timeoutJob = null

        view?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing contact picker overlay", e)
            }
        }
        view = null

        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    companion object {
        private const val TAG = "ContactPickerOverlay"
        private const val TIMEOUT_MS = 15_000L
        private const val WIDTH_FRACTION = 0.9f
    }
}