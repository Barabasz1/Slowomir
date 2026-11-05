package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridView
import android.accessibilityservice.AccessibilityService
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.text.BoringLayout
import android.view.Gravity
import android.view.WindowManager
import com.zpi.slowomir.touch.TouchExecutor
import com.zpi.slowomir.utils.ScreenPoint


class GridExecutor (
    private val service: AccessibilityService,
    private val touchExecutor: TouchExecutor
) {
    companion object {
        const val DEFAULT_ROWS = 10
        const val DEFAULT_COLS = 6
    }
    private var gridView: GridView? = null
    private var parentCell: Int? = null
    private val divisions = mutableListOf<Pair<Int, Int>>()
    private var selectedCell: Int? = 15

    fun isEnabled() : Boolean {
        return gridView != null
    }

    fun isCellSelected() : Boolean {
        return selectedCell != null
    }

    fun canExecuteActionOnCell() : Boolean {
        return isCellSelected() && isEnabled()
    }
    fun enable() {
        if (gridView != null) return

        val windowManager =
            service.getSystemService(WINDOW_SERVICE) as WindowManager

        val grid = GridView(
            context = service,
            rows = DEFAULT_ROWS,
            cols = DEFAULT_COLS
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
        selectedCell = 15
        redraw()
    }

    fun disable() {
        val windowManager =
            service.getSystemService(WINDOW_SERVICE) as WindowManager
        gridView?.let { windowManager.removeView(it) }
        gridView = null
        parentCell = null
        selectedCell = null
        divisions.clear()
    }

    fun toggle() {
        if (gridView == null) enable() else disable()
    }

    private fun redraw() {
        gridView?.update(selectedCell, parentCell, divisions.toList())
    }

    //TODO check if cell is in grid
    fun selectCell(cell: Int) {
        selectedCell = cell
    }
    private fun divide(cell: Int, n: Int) : Boolean {
        if (gridView == null) return false

        require(n >= 2)

        val maxCell = divisions.lastOrNull()?.second?.let { it * it }
            ?: ((gridView?.rows ?: DEFAULT_ROWS) * (gridView?.cols ?: DEFAULT_COLS))

        require(cell in 1..maxCell)

        if (parentCell != cell) {
            divisions.clear()
        }

        parentCell = cell
        selectedCell = null
        divisions.add(cell to n)
        redraw()
        return true
    }
    fun divide(n: Int) : Boolean {
        return selectedCell != null && divide(selectedCell!!, n)
    }

    fun getTouchCoords(cell: Int): ScreenPoint {
        val view = gridView ?: return ScreenPoint(0f, 0f)
        val cols = view.cols
        val rows = view.rows

        if (divisions.isEmpty()) {
            val cellIndex = cell - 1
            val row = cellIndex / cols
            val col = cellIndex % cols

            val cw = view.width.toFloat() / cols
            val ch = view.height.toFloat() / rows

            val x = (col + 0.5f) * cw
            val y = (row + 0.5f) * ch
            return ScreenPoint(x, y)
        }

        val rootCell = divisions.first().first - 1
        val rootRow = rootCell / cols
        val rootCol = rootCell % cols

        var cellX = rootCol * (view.width.toFloat() / cols)
        var cellY = rootRow * (view.height.toFloat() / rows)
        var cellW = view.width.toFloat() / cols
        var cellH = view.height.toFloat() / rows

        for (i in 0 until divisions.size - 1) {
            val subCell = divisions[i + 1].first - 1
            val n = divisions[i].second
            val subRow = subCell / n
            val subCol = subCell % n

            cellW /= n
            cellH /= n
            cellX += subCol * cellW
            cellY += subRow * cellH
        }

        val lastN = divisions.last().second
        val targetIndex = cell - 1
        val targetRow = targetIndex / lastN
        val targetCol = targetIndex % lastN

        val subW = cellW / lastN
        val subH = cellH / lastN
        val finalX = cellX + (targetCol + 0.5f) * subW
        val finalY = cellY + (targetRow + 0.5f) * subH

        return ScreenPoint(finalX, finalY)
    }

    private fun tapCell(cell: Int): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.shortTap(x,y)
    }
    fun tapCell() : Boolean {
        return selectedCell != null && tapCell(selectedCell!!)
    }

    private fun longTapCell(cell: Int, duration: Long = 1000): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.longTap(x,y, duration)
    }
    fun longTapCell(duration: Long = 1000) : Boolean {
        return selectedCell != null && longTapCell(selectedCell!!, duration)
    }

    private fun swipeUpCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeUp(x, y, distance)
    }
    fun swipeUpCell(distance: Float = 500f) : Boolean {
        return selectedCell != null && swipeUpCell(selectedCell!!, distance)
    }

    private fun swipeDownCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeDown(x, y, distance)
    }
    fun swipeDownCell(distance: Float = 500f) : Boolean {
        return selectedCell != null && swipeDownCell(selectedCell!!, distance)
    }

    private fun swipeRightCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeRight(x, y, distance)
    }
    fun swipeRightCell(distance: Float = 500f) : Boolean {
        return selectedCell != null && swipeRightCell(selectedCell!!, distance)
    }

    private fun swipeLeftCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeLeft(x, y, distance)
    }
    fun swipeLeftCell(distance: Float = 500f) : Boolean {
        return selectedCell != null && swipeLeftCell(selectedCell!!, distance)
    }

    private fun pinchInCell(cell: Int, power: Float = 1f): Boolean {
        if (gridView == null) return false

        val screenPoint: ScreenPoint = getTouchCoords(cell)
        return touchExecutor.pinchIn(screenPoint.x,screenPoint.y, power)
    }
    fun pinchInCell(power: Float = 1f) : Boolean {
        return selectedCell != null && pinchInCell(selectedCell!!, power)
    }

    private fun pinchOutCell(cell: Int, power: Float = 1f): Boolean {
        if (gridView == null) return false

        val screenPoint: ScreenPoint = getTouchCoords(cell)
        return touchExecutor.pinchOut(screenPoint.x,screenPoint.y, power)
    }
    fun pinchOutCell(power: Float = 1f) : Boolean {
        return selectedCell != null && pinchOutCell(selectedCell!!, power)
    }
}
