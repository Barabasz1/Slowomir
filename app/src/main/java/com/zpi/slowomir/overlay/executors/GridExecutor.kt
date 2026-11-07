package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridView
import android.accessibilityservice.AccessibilityService
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.touch.LongTapCommand
import com.zpi.slowomir.overlay.commands.touch.PinchInCommand
import com.zpi.slowomir.overlay.commands.touch.PinchOutCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeDownCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeLeftCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeRightCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeUpCommand
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.touch.TouchExecutor.Companion.DEFAULT_HOLD_DURATION
import com.zpi.slowomir.touch.TouchExecutor.Companion.DEFAULT_PINCH_POWER
import com.zpi.slowomir.touch.TouchExecutor.Companion.DEFAULT_SWIPE_DISTANCE
import com.zpi.slowomir.utils.ScreenPoint
import com.zpi.slowomir.voice.mediator.CommandMediator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridExecutor @Inject constructor(
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val longTapCommandFactory: LongTapCommand.Factory,
    private val swipeLeftCommandFactory: SwipeLeftCommand.Factory,
    private val swipeRightCommandFactory: SwipeRightCommand.Factory,
    private val swipeDownCommandFactory: SwipeDownCommand.Factory,
    private val swipeUpCommandFactory: SwipeUpCommand.Factory,
    private val pinchInCommandFactory: PinchInCommand.Factory,
    private val pinchOutCommandFactory: PinchOutCommand.Factory,
) {
    companion object {
        const val DEFAULT_ROWS = 10
        const val DEFAULT_COLS = 6
    }

    private var accessibilityService: AccessibilityService? = null
    private var gridView: GridView? = null
    private var parentCell: Int? = null
    private val divisions = mutableListOf<Pair<Int, Int>>()
    private var selectedCell: Int? = null

    fun attachService(accessibilityService: AccessibilityService) {
        this.accessibilityService = accessibilityService
    }

    fun detachService() {
        this.accessibilityService = null
    }

    fun isEnabled(): Boolean {
        return gridView != null
    }

    fun isCellSelected(): Boolean {
        return selectedCell != null
    }

    fun canExecuteActionOnCell(): Boolean {
        return isCellSelected() && isEnabled()
    }

    fun enable() {
        if (gridView != null) return
        val service = accessibilityService ?: return

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
        selectedCell = null
        redraw()
    }

    fun disable() {
        val service = accessibilityService
        if (service != null) {
            val windowManager =
                service.getSystemService(WINDOW_SERVICE) as WindowManager
            gridView?.let { windowManager.removeView(it) }
        }

        gridView = null
        parentCell = null
        selectedCell = null
        divisions.clear()
    }

    fun toggle() {
        if (gridView == null) enable() else disable()
    }

    fun reset() {
        selectedCell = null
        parentCell = null
        divisions.clear()
        redraw()
    }

    private fun redraw() {
        gridView?.update(selectedCell, parentCell, divisions.toList())
    }

    fun selectCell(cell: Int) {
        val maxCell = if (divisions.isEmpty()) {
            (gridView?.cols ?: DEFAULT_COLS) * (gridView?.rows ?: DEFAULT_ROWS)
        } else {
            divisions.last().second * divisions.last().second
        }
        if (cell in 1..maxCell) {
            selectedCell = cell
            redraw()
        }
    }

    private fun divide(cell: Int, n: Int): Boolean {
        if (gridView == null) return false

        require(n >= 2)

        val maxCell = divisions.lastOrNull()?.second?.let { it * it }
            ?: ((gridView?.rows ?: DEFAULT_ROWS) * (gridView?.cols ?: DEFAULT_COLS))

        require(cell in 1..maxCell)

        if (divisions.isEmpty()) {
            parentCell = cell
        }

        selectedCell = null
        divisions.add(cell to n)
        redraw()
        return true
    }

    fun divide(n: Int): Boolean {
        return selectedCell != null && divide(selectedCell!!, n)
    }

    fun getTouchCoords(cell: Int): ScreenPoint {
        val view = gridView ?: return ScreenPoint(0f, 0f)
        val cols = view.cols
        val rows = view.rows

        val maxCell = if (divisions.isEmpty()) {
            cols * rows
        } else {
            divisions.last().second * divisions.last().second
        }
        if (cell !in 1..maxCell) return ScreenPoint(0f, 0f)

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

    private inline fun dispatchCellCommand(
        cell: Int,
        commandFactory: (x: Float, y: Float) -> Command
    ): Boolean {
        if (gridView == null) return false
        val (x, y) = getTouchCoords(cell)
        mediator.dispatchScoped(commandFactory(x, y))
        reset()
        return true
    }

    private fun tapCell(cell: Int): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            tapCommandFactory.create(x, y)
        }
    }

    fun tapCell(): Boolean {
        return selectedCell != null && tapCell(selectedCell!!)
    }

    private fun longTapCell(cell: Int, duration: Long = DEFAULT_HOLD_DURATION): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            longTapCommandFactory.create(x, y, duration)
        }
    }

    fun longTapCell(duration: Long = DEFAULT_HOLD_DURATION): Boolean {
        return selectedCell != null && longTapCell(selectedCell!!, duration)
    }

    private fun swipeUpCell(cell: Int, distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            swipeUpCommandFactory.create(x, y, distance)
        }
    }

    fun swipeUpCell(distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return selectedCell != null && swipeUpCell(selectedCell!!, distance)
    }

    private fun swipeDownCell(cell: Int, distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            swipeDownCommandFactory.create(x, y, distance)
        }
    }

    fun swipeDownCell(distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return selectedCell != null && swipeDownCell(selectedCell!!, distance)
    }

    private fun swipeRightCell(cell: Int, distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            swipeRightCommandFactory.create(x, y, distance)
        }
    }

    fun swipeRightCell(distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return selectedCell != null && swipeRightCell(selectedCell!!, distance)
    }

    private fun swipeLeftCell(cell: Int, distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            swipeLeftCommandFactory.create(x, y, distance)
        }
    }

    fun swipeLeftCell(distance: Float = DEFAULT_SWIPE_DISTANCE): Boolean {
        return selectedCell != null && swipeLeftCell(selectedCell!!, distance)
    }

    //TODO
    fun swipe(cell0: Int, cell1: Int): Boolean {
        return true
    }

    private fun pinchInCell(cell: Int, power: Float = DEFAULT_PINCH_POWER): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            pinchInCommandFactory.create(x, y, power)
        }
    }

    fun pinchInCell(power: Float = DEFAULT_PINCH_POWER): Boolean {
        return selectedCell != null && pinchInCell(selectedCell!!, power)
    }

    private fun pinchOutCell(cell: Int, power: Float = DEFAULT_PINCH_POWER): Boolean {
        return dispatchCellCommand(cell) { x, y ->
            pinchOutCommandFactory.create(x, y, power)
        }
    }

    fun pinchOutCell(power: Float = DEFAULT_PINCH_POWER): Boolean {
        return selectedCell != null && pinchOutCell(selectedCell!!, power)
    }
}