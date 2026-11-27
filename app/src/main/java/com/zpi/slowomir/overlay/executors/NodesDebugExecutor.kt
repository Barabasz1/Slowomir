package com.zpi.slowomir.overlay.executors

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.zpi.slowomir.overlay.ui.common.AccessibilityUiTreeSource
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.overlay.ui.labels.NodesDebugView
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
 * Developer overlay that outlines every accessibility node of the screen and tags it with its
 * class and properties.
 */
@Singleton
class NodesDebugExecutor @Inject constructor(
    hostProvider: OverlayHostProvider,
    private val uiTree: AccessibilityUiTreeSource,
) : OverlayWindowManager(hostProvider) {

    override val tag = "NodesDebugOverlay"

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var refreshJob: Job? = null
    private var nodesView: NodesDebugView? = null

    override fun onCreateView(context: Context): View = NodesDebugView(context)

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
        nodesView = view as NodesDebugView
        refresh()
        refreshJob?.cancel()
        refreshJob = scope.launch {
            uiTree.changes.debounce(REFRESH_DEBOUNCE_MS).collect { refresh() }
        }
    }

    override fun onViewDetached(view: View) {
        if (nodesView === view) nodesView = null
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun refresh() {
        nodesView?.update(uiTree.snapshot().nodes)
    }

    private companion object {
        const val REFRESH_DEBOUNCE_MS = 150L
    }
}
