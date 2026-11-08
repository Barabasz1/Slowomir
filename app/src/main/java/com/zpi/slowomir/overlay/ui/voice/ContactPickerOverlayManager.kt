package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.ui.ContactPickerView
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.ContactSelectionManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ContactPickerOverlayManager @Inject constructor(
    private val mediator: CommandMediator,
    private val contactSelectionManager: ContactSelectionManager,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {

    override val tag = "ContactPickerOverlay"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var timeoutJob: Job? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private data class PickerState(
        val options: List<ContactOption> = emptyList(),
        val action: CommandIdentifier? = null,
    )

    private val _pickerState = MutableStateFlow(PickerState())

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            mediator.mediatorState
                .map { it.contactSelection to it.contactSelectionAction }
                .distinctUntilChanged()
                .collect { (options, action) ->
                    if (options.isNullOrEmpty()) {
                        hide()
                    } else {
                        showOptions(options, action)
                    }
                }
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
        _pickerState.value = PickerState()
        hide()
    }

    private fun showOptions(options: List<ContactOption>, action: CommandIdentifier?) {
        hide()

        _pickerState.value = PickerState(options = options, action = action)

        if (!show()) {
            contactSelectionManager.cancelSelection()
        }

        timeoutJob = scope.launch {
            delay(TIMEOUT_MS.milliseconds)
            Log.i(tag, "Contact selection timed out")
            contactSelectionManager.cancelSelection()
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
                val state by _pickerState.collectAsState()
                SłowomirTheme {
                    ContactPickerView(
                        options = state.options,
                        action = state.action,
                        onSelect = { index -> contactSelectionManager.selectContact(index) },
                        onCancel = { contactSelectionManager.cancelSelection() },
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
            contactSelectionManager.cancelSelection()
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
