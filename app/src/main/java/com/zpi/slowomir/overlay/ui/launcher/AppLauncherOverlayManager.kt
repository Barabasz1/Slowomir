package com.zpi.slowomir.overlay.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.executors.AppItem
import com.zpi.slowomir.overlay.ui.common.OverlayHost
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.voice.VoiceSessionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
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
class AppLauncherOverlayManager @Inject constructor(
    private val mediator: CommandMediator,
    private val voiceSessionManager: VoiceSessionManager,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {
    override val tag = "AppLauncherOverlayManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var timeoutJob: Job? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var options: List<AppItem> = emptyList()

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            mediator.mediatorState
                .map { it.appSelection }
                .distinctUntilChanged()
                .collect { options ->
                    if (options.isNullOrEmpty()) {
                        hide()
                    } else {
                        showOptions(options)
                    }
                }
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
        hide()
    }

    private fun showOptions(options: List<AppItem>) {
        hide()
        this.options = options

        if (!show()) {
            voiceSessionManager.cancelAppSelection()
        }

        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(TIMEOUT_MS)
            Log.i(tag, "App selection timed out")
            voiceSessionManager.cancelAppSelection()
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
                SłowomirTheme {
                    AppPickerView(
                        options = options,
                        onSelect = { position -> voiceSessionManager.selectApp(position) },
                        onCancel = { voiceSessionManager.cancelAppSelection() }
                    )
                }
            }
        }
    }

    override fun onCreateLayoutParams(windowType: Int): LayoutParams {
        val width = (Resources.getSystem().displayMetrics.widthPixels * WIDTH_FRACTION).toInt()
        return LayoutParams(
            width,
            LayoutParams.WRAP_CONTENT,
            windowType,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    override fun onViewAttached(view: View) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(TIMEOUT_MS)
            Log.i(tag, "App selection timed out")
            voiceSessionManager.cancelAppSelection()
        }
    }

    override fun onViewDetached(view: View) {
        timeoutJob?.cancel()
        timeoutJob = null
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

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val WIDTH_FRACTION = 0.9f
    }
}
