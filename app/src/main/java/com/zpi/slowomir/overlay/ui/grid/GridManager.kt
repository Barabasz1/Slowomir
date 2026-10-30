package com.zpi.slowomir.overlay.ui.grid

import android.app.Service
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager

class GridManager(private val service: Service) {
    private val windowManager =
        service.getSystemService(WINDOW_SERVICE) as WindowManager

    private var gridView: GridView? = null
    private var selectedCell: Int? = null
    private val divisions = mutableListOf<Pair<Int, Int>>()

    fun enable() {
        if (gridView != null) return

        val grid = GridView(
            context = service,
            rows = 10,
            cols = 6,
            lineOpacity = 180,
            labelOpacity = 220,
            overlayOpacity = 0,
            lineWidthPx = 2f,
            labelTextSizePx = 32f
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(grid, params)
        gridView = grid
        redraw()
    }

    fun disable() {
        gridView?.let { windowManager.removeView(it) }
        gridView = null
        selectedCell = null
        divisions.clear()
    }

    fun toggle() {
        if (gridView == null) enable() else disable()
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