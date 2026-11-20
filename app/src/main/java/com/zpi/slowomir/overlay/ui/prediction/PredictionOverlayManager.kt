package com.zpi.slowomir.overlay.ui.prediction

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionOverlayManager @Inject constructor(
    hostProvider: OverlayHostProvider,
) : OverlayWindowManager(hostProvider) {

    override val tag = "PredictionOverlayManager"
    private var highlightView: PredictionHighlightView? = null
    private var pendingBounds: Rect? = null

    override fun onCreateView(context: Context): View {
        return PredictionHighlightView(context)
    }

    override fun onCreateLayoutParams(windowType: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    override fun onViewAttached(view: View) {
        highlightView = view as? PredictionHighlightView
        pendingBounds?.let {
            highlightView?.setTarget(it)
        }
    }

    override fun onViewDetached(view: View) {
        if (highlightView === view) {
            highlightView = null
        }
    }

    suspend fun highlight(bounds: Rect) = withContext(Dispatchers.Main.immediate) {
        pendingBounds = bounds
        show()
        highlightView?.setTarget(bounds)
    }

    suspend fun clearHighlight() = withContext(Dispatchers.Main.immediate) {
        pendingBounds = null
        highlightView?.clearTarget()
        hide()
    }
}
