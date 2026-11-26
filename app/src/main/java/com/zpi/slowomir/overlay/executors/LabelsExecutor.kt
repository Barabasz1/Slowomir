package com.zpi.slowomir.overlay.executors

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.zpi.slowomir.overlay.ui.common.AccessibilityUiTreeSource
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.overlay.ui.labels.LabelsView
import com.zpi.slowomir.overlay.ui.labels.toLabelTargets
import com.zpi.slowomir.overlay.commands.touch.LongTapCommand
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.UiNodeDescriber
import com.zpi.slowomir.overlay.ui.labels.LabelTarget
import com.zpi.slowomir.overlay.ui.labels.findByText
import com.zpi.slowomir.utils.PreferencesManager
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows numbered labels over the smallest non-overlapping interactive fragments of the screen.
 * The labels follow the screen while the overlay is visible.
 */
@Singleton
class LabelsExecutor @Inject constructor(
    hostProvider: OverlayHostProvider,
    private val uiTree: AccessibilityUiTreeSource,
    private val preferencesManager: PreferencesManager,
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val longTapCommandFactory: LongTapCommand.Factory,
    private val uiNodeDescriber: UiNodeDescriber,
) : OverlayWindowManager(hostProvider) {

    override val tag = "LabelsOverlay"

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var refreshJob: Job? = null
    private var gridTypeJob: Job? = null
    private var labelsView: LabelsView? = null
    private var targets: List<LabelTarget> = emptyList()
    private var lastTarget: ActionDescriptor? = null

    fun consumeLastTarget(): ActionDescriptor? = lastTarget.also { lastTarget = null }

    /**
     * Taps the label with the given [number] or, when there is none, the label whose text matches [text].
     *
     * @return false when the overlay is hidden or no such label is shown.
     */
    fun select(number: Int?, text: String?, long: Boolean = false): Boolean {
        if (labelsView == null) return false
        val target = when {
            number != null -> targets.firstOrNull { it.number == number }
            text != null -> targets.findByText(text)
            else -> null
        } ?: return false

        val x = (target.bounds.left + target.bounds.right) / 2f
        val y = (target.bounds.top + target.bounds.bottom) / 2f
        lastTarget = uiNodeDescriber.describeTapAt(x, y)
        val command = if (long) {
            longTapCommandFactory.create(x, y, TouchExecutor.DEFAULT_HOLD_DURATION)
        } else {
            tapCommandFactory.create(x, y)
        }
        mediator.dispatchScoped(command, logged = false)
        return true
    }

    override fun onCreateView(context: Context): View =
        LabelsView(context, gridType = preferencesManager.gridType.value)

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

    @OptIn(FlowPreview::class)
    override fun onViewAttached(view: View) {
        labelsView = view as LabelsView
        refresh()
        refreshJob?.cancel()
        refreshJob = scope.launch {
            uiTree.changes.debounce(REFRESH_DEBOUNCE_MS).collect { refresh() }
        }
        gridTypeJob?.cancel()
        gridTypeJob = scope.launch {
            preferencesManager.gridType.collect { refresh() }
        }
    }

    override fun onViewDetached(view: View) {
        if (labelsView === view) labelsView = null
        targets = emptyList()
        refreshJob?.cancel()
        refreshJob = null
        gridTypeJob?.cancel()
        gridTypeJob = null
    }

    private fun refresh() {
        val view = labelsView ?: return
        val minSide = (MIN_SIDE_DP * view.resources.displayMetrics.density).toInt()
        targets = uiTree.snapshot().toLabelTargets(minSidePx = minSide)
        view.update(targets, preferencesManager.gridType.value)
    }

    private companion object {
        const val MIN_SIDE_DP = 24
        const val REFRESH_DEBOUNCE_MS = 150L
    }
}
