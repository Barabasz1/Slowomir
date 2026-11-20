package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.common.FocusLayout
import com.zpi.slowomir.overlay.ui.common.FocusLayoutSource
import com.zpi.slowomir.overlay.ui.common.OverlayHost
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayPlacementPolicy
import com.zpi.slowomir.overlay.ui.common.OverlaySide
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.ui.theme.SłowomirTheme
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.stt.VoiceState
import com.zpi.slowomir.voice.stt.VoiceStateSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource


/**
 * UI Window Manager that renders the floating [VoiceOverlayView] on top of system interfaces.
 * Displays live speech-to-text transcriptions and pipeline execution states in real-time.
 */
@Singleton
class VoiceOverlayManager @Inject constructor(
    private val mediator: CommandMediator,
    private val voiceStateSource: VoiceStateSource,
    private val focusLayoutSource: FocusLayoutSource,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {

    override val tag = "VoiceOverlayManager"
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trackingJob: Job? = null

    private var lastLoggedLayout: FocusLayout? = null

    private var side = OverlaySide.TOP
    private var edgeOffset = 0

    private var topInset = 0

    val state: StateFlow<OverlayState> = mediator.mediatorState
        .map { it.overlayState }
        .distinctUntilChanged()
        .withMinDisplayTime(MIN_DISPLAY.milliseconds)
        .hideResultsAfter(RESULT_VISIBLE.milliseconds)
        .stateIn(scope, SharingStarted.Eagerly, OverlayState.Idle)


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
                    VoiceOverlayView(state = state)
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

    override fun onViewAttached(view: View) {
        side = OverlaySide.TOP
        edgeOffset = 0
        trackFocusedField()
    }

    override fun onViewDetached(view: View) {
        trackingJob?.cancel()
        trackingJob = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

        private fun trackFocusedField() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            voiceStateSource.state.collectLatest { state ->
                if (!state.isVoiceActive()) {
                    withContext(Dispatchers.Main.immediate) { reposition(FocusLayout()) }
                    return@collectLatest
                }

                while (true) {
                    val layout = focusLayoutSource.read()
                    withContext(Dispatchers.Main.immediate) { reposition(layout) }
                    delay(TRACKING_INTERVAL_MS)
                }
            }
        }
    }

    private fun reposition(layout: FocusLayout) {
        val view = attachedView ?: return
        val windowManager = view.context.getSystemService(WindowManager::class.java) ?: return
        val density = view.resources.displayMetrics.density

        val insets = view.rootWindowInsets?.getInsets(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
        )

        if (side == OverlaySide.TOP) {
            topInset = IntArray(2).also { view.getLocationOnScreen(it) }[1]
        }

        val placement = OverlayPlacementPolicy.choose(
            layout = layout,
            screenHeight = windowManager.currentWindowMetrics.bounds.height(),
            topInset = maxOf(insets?.top ?: 0, topInset),
            bottomInset = insets?.bottom ?: 0,
            overlayHeight = maxOf(view.height, (CARD_HEIGHT_DP * density).toInt()),
            gap = (GAP_DP * density).toInt(),
            current = side,
        )

        if (layout != lastLoggedLayout) {
            lastLoggedLayout = layout
            Log.d(tag, "[PLACEMENT] $layout screenH=${windowManager.currentWindowMetrics.bounds.height()} " +
                "insets(top=${maxOf(insets?.top ?: 0, topInset)}, bottom=${insets?.bottom ?: 0}) " +
                "overlayH=${view.height} current=$side -> $placement")
        }

        if (placement.side == side && placement.edgeOffset == edgeOffset) return

        side = placement.side
        edgeOffset = placement.edgeOffset
        updateLayout {
            gravity = (if (side == OverlaySide.TOP) Gravity.TOP else Gravity.BOTTOM) or Gravity.CENTER_HORIZONTAL
            y = edgeOffset
        }
    }

    private fun VoiceState.isVoiceActive() = this !is VoiceState.Stopped &&
        this !is VoiceState.Failed && this !is VoiceState.KeywordSpotting

    override fun onAttachDenied(host: OverlayHost) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${host.context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { host.context.startActivity(intent) }
            .onFailure { Log.e(tag, "Failed to open overlay permission screen", it) }
    }

    private companion object {
        const val TRACKING_INTERVAL_MS = 500L
        const val CARD_HEIGHT_DP = 72
        const val GAP_DP = 8
        const val RESULT_VISIBLE = 5_000
        const val MIN_DISPLAY = 1_000
    }

    suspend fun showOverlay(): Boolean = withContext(Dispatchers.Main.immediate) { show() }
    suspend fun hideOverlay() = withContext(Dispatchers.Main.immediate) { hide() }
}

internal fun Flow<OverlayState>.withMinDisplayTime(minDisplay: Duration): Flow<OverlayState> = flow {
    var previous: OverlayState = OverlayState.Idle
    var shownAt = TimeSource.Monotonic.markNow()
    buffer(Channel.UNLIMITED).collect { next ->
        if (next::class != previous::class) {
            if (!previous.isHidden && next !is OverlayState.ProcessingCommand) {
                val remaining = minDisplay - shownAt.elapsedNow()
                if (remaining.isPositive()) delay(remaining)
            }
            shownAt = TimeSource.Monotonic.markNow()
        }
        previous = next
        emit(next)
    }
}

internal fun Flow<OverlayState>.hideResultsAfter(after: Duration): Flow<OverlayState> = transformLatest { state ->
    emit(state)
    if (state.isResult) {
        delay(after)
        emit(OverlayState.Idle)
    }
}
