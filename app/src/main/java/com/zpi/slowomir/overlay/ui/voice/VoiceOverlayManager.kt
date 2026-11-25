package com.zpi.slowomir.overlay.ui.voice

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zpi.slowomir.overlay.ui.common.AccessibilityUiTreeSource
import com.zpi.slowomir.overlay.ui.common.Obstacle
import com.zpi.slowomir.overlay.ui.common.settled
import com.zpi.slowomir.overlay.ui.common.toAvoidByOverlay
import com.zpi.slowomir.overlay.ui.common.toObstacles
import com.zpi.slowomir.overlay.ui.common.OverlayHost
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayLifecycleOwner
import com.zpi.slowomir.overlay.ui.common.OverlayPlacement
import com.zpi.slowomir.overlay.ui.common.OverlayShape
import com.zpi.slowomir.overlay.ui.common.OverlaySide
import com.zpi.slowomir.overlay.ui.common.ObstacleKind
import com.zpi.slowomir.overlay.ui.common.ScreenGeometry
import com.zpi.slowomir.overlay.ui.common.SlidingWindowSearch
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
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
    private val uiTree: AccessibilityUiTreeSource,
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {

    override val tag = "VoiceOverlayManager"
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trackingJob: Job? = null
    private var keepScreenOnJob: Job? = null
    private var applied: OverlayPlacement? = null
    private val appliedSide: OverlaySide get() = applied?.side ?: OverlaySide.TOP
    private val appliedOffset: Int get() = applied?.offset ?: 0

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
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
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
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    }

    override fun onViewAttached(view: View) {
        applied = null
        trackFocusedField()
        keepScreenOnJob?.cancel()
        keepScreenOnJob = scope.launch {
            combine(state, voiceStateSource.state) { overlay, voice ->
                overlay.keepsScreenOn() || voice.isTextSession()
            }
                .distinctUntilChanged()
                .collect { keepOn -> withContext(Dispatchers.Main.immediate) { setKeepScreenOn(keepOn) } }
        }
    }

    override fun onViewDetached(view: View) {
        trackingJob?.cancel()
        trackingJob = null
        keepScreenOnJob?.cancel()
        keepScreenOnJob = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    private fun trackFocusedField() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            voiceStateSource.state.collectLatest { voiceState ->
                if (!voiceState.isVoiceActive()) {
                    withContext(Dispatchers.Main.immediate) { reposition(emptyList(), keepCurrent = false) }
                    return@collectLatest
                }

                suspend fun refresh(): Boolean {
                    val obstacles = uiTree.snapshot().toObstacles()
                    return withContext(Dispatchers.Main.immediate) { reposition(obstacles) }
                }

                if (applied == null) refresh()
                merge(uiTree.changes, fallbackTicks())
                    .onStart { emit(Unit) }
                    .settled(SETTLE_MS.milliseconds, MAX_SETTLE_MS.milliseconds)
                    .collect { if (refresh() && !state.value.isHidden) delay(MIN_DWELL_MS.milliseconds) }
            }
        }
    }

    private fun OverlayState.keepsScreenOn() = !isHidden && this !is OverlayState.ListeningForCommand

    private fun VoiceState.isTextSession() =
        this is VoiceState.Dictating || this is VoiceState.Editing || this is VoiceState.TextProcessing

    private fun setKeepScreenOn(keepOn: Boolean) {
        updateLayout {
            flags = if (keepOn) flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            else flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        }
    }

    private fun fallbackTicks() = flow {
        while (true) {
            delay(FALLBACK_INTERVAL_MS)
            emit(Unit)
        }
    }

    /**
     * @param keepCurrent false puts the overlay back to its default place instead of staying where it is.
     * @return true when the overlay was moved.
     */
    private fun reposition(obstacles: List<Obstacle>, keepCurrent: Boolean = true): Boolean {
        val view = attachedView ?: return false
        val density = view.resources.displayMetrics.density

        val hidden = state.value.isHidden
        val found = measureScreen(view, obstacles)?.let { screen ->
            SlidingWindowSearch.place(
                shape = OverlayShape.Bar((CARD_HEIGHT_DP * density).toInt()),
                screen = screen,
                obstacles = obstacles.toAvoidByOverlay(
                    tinyPx = (TINY_DP * density).toInt(),
                    fullPx = (FULL_SIZE_DP * density).toInt(),
                    fieldGapPx = (GAP_DP * density).toInt(),
                ),
                step = (SEARCH_STEP_DP * density).toInt().coerceAtLeast(1),
                current = applied?.position.takeIf { keepCurrent && !hidden },
            )
        }
        val newSide = found?.side ?: OverlaySide.TOP
        val newOffset = found?.offset ?: 0

        val small = newSide == appliedSide && abs(newOffset - appliedOffset) < MIN_MOVE_DP * density
        if (keepCurrent && small && !hidden) return false

        val unchanged = newSide == appliedSide && newOffset == appliedOffset
        applied = found
        if (unchanged) return false

        updateLayout {
            gravity = (if (newSide == OverlaySide.TOP) Gravity.TOP else Gravity.BOTTOM) or Gravity.CENTER_HORIZONTAL
            y = newOffset
        }

        return true
    }

    private fun measureScreen(view: View, obstacles: List<Obstacle>): ScreenGeometry? {
        val metrics = view.context.getSystemService(WindowManager::class.java)?.currentWindowMetrics ?: return null
        val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        return ScreenGeometry(
            width = metrics.bounds.width(),
            height = metrics.bounds.height(),
            topInset = insets.top,
            bottomInset = insets.bottom,
            keyboardTop = obstacles.firstOrNull { it.kind == ObstacleKind.KEYBOARD }?.bounds?.top,
        )
    }

    private fun VoiceState.isVoiceActive() = this !is VoiceState.Stopped &&
        this !is VoiceState.Failed && this !is VoiceState.KeywordSpotting

    override fun onAttachDenied(host: OverlayHost) {
        Log.w(tag, "Overlay permission not granted, overlay not shown")
    }

    private companion object {
        const val SEARCH_STEP_DP = 8

        const val TINY_DP = 12
        const val FULL_SIZE_DP = 40
        const val SETTLE_MS = 500L

        const val MAX_SETTLE_MS = 1000L
        const val MIN_DWELL_MS = 2000L
        const val FALLBACK_INTERVAL_MS = 3000L
        const val CARD_HEIGHT_DP = 72

        const val MIN_MOVE_DP = 40
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
