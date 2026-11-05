package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.ContactPickerView
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.utils.ContactOption
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
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ContactPickerOverlayManager @Inject constructor(
    private val mediator: CommandMediator,
    private val voiceSessionManager: VoiceSessionManager,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {

    override val tag = "ContactPickerOverlay"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var timeoutJob: Job? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private var options: List<ContactOption> = emptyList()

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            mediator.mediatorState
                .map { it.contactSelection }
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

    private fun showOptions(options: List<ContactOption>) {
        hide()

        this.options = options

        if (!show()) {
            voiceSessionManager.cancelSelection()
        }

        timeoutJob = scope.launch {
            delay(TIMEOUT_MS.milliseconds)
            Log.i(tag, "Contact selection timed out")
            voiceSessionManager.cancelSelection()
        }
    }

    override fun onCreateView(context: Context): View {
        val lifecycle = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        return ComposeView(context).apply {
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

    }

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams {
        val width = (Resources.getSystem().displayMetrics.widthPixels * WIDTH_FRACTION).toInt()
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    override fun onViewAttached(view: View) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(TIMEOUT_MS.milliseconds)
            Log.i(tag, "Contact selection timed out")
            voiceSessionManager.cancelSelection()
        }
    }

    override fun onViewDetached(view: View) {
        timeoutJob?.cancel()
        timeoutJob = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
        private const val WIDTH_FRACTION = 0.9f
    }
}