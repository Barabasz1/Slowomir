package com.zpi.slowomir.overlay.ui.grid

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridManager @Inject constructor(
    hostProvider: OverlayHostProvider
) : OverlayWindowManager(hostProvider) {
    override val tag = "GridManager"
    private var gridView: GridView? = null
    private var selectedCell: Int? = null
    private val divisions = mutableListOf<Pair<Int, Int>>()

    override fun onCreateView(context: Context): View {
        return GridView(
            context = context,
            rows = 10,
            cols = 6,
            lineOpacity = 180,
            labelOpacity = 220,
            overlayOpacity = 0,
            lineWidthPx = 2f,
            labelTextSizePx = 32f
        )
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
        gridView = view as GridView
        redraw()
    }

    override fun onViewDetached(view: View) {
        if (gridView === view) {
            gridView = null
        }
    }

    fun enable() {
        show()
        Log.i(tag, "Grid overlay enabled")
    }

    fun disable() {
        hide()
        selectedCell = null
        divisions.clear()
        Log.i(tag, "Grid overlay disabled")
    }

    override fun toggle(): Boolean {
        if (isShowing) disable() else enable()
        return isShowing
    }

    fun selectCell(cell: Int) {
        val maxCell = divisions.lastOrNull()?.second?.let { it * it }
            ?: ((gridView?.rows ?: 10) * (gridView?.cols ?: 6))

        require(cell in 1..maxCell)

        if (divisions.isEmpty() || divisions.last().first != cell) {
            divisions.clear()
        }

        selectedCell = cell
        redraw()
    }

    fun divide(cell: Int, n: Int) {
        if (gridView == null) return

        require(n >= 2)

        val maxCell = divisions.lastOrNull()?.second?.let { it * it }
            ?: ((gridView?.rows ?: 10) * (gridView?.cols ?: 6))

        require(cell in 1..maxCell)

        if (selectedCell != cell) {
            divisions.clear()
        }

        selectedCell = cell
        divisions.add(cell to n)
        redraw()
    }

    private fun redraw() {
        gridView?.update(selectedCell, divisions.toList())
    }
}