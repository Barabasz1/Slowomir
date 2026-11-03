package com.zpi.slowomir.overlay.ui.grid

import android.app.Service
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.zpi.slowomir.overlay.commands.LongTapCommand
import com.zpi.slowomir.overlay.commands.PinchInCommand
import com.zpi.slowomir.overlay.commands.PinchOutCommand
import com.zpi.slowomir.overlay.commands.SwipeCommand
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.touch.TouchExecutor
import com.zpi.slowomir.utils.ScreenPoint
import com.zpi.slowomir.voice.mediator.CommandMediator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridManager @Inject constructor(
    private val tapCommandFactory: TapCommand.Factory,
    private val longTapCommandFactory: LongTapCommand.Factory,
    private val swipeCommandFactory: SwipeCommand.Factory,
    private val pinchInCommandFactory: PinchInCommand.Factory,
    private val pinchOutCommandFactory: PinchOutCommand.Factory,
    private val mediator: CommandMediator,
    private val touchExecutor: TouchExecutor
) {
    companion object {
        const val DEFAULT_ROWS = 10
        const val DEFAULT_COLS = 6
        const val DEFAULT_LINE_OPACITY = 180
        const val DEFAULT_LABEL_OPACITY = 220
        const val DEFAULT_OVERLAY_OPACITY = 0
        const val DEFAULT_LINE_WIDTH_PX = 2f
        const val DEFAULT_LABEL_TEXT_SIZE_PX = 32f
    }

    private var service: Service? = null
    private var gridView: GridView? = null
    private var selectedCell: Int? = null
    private val divisions = mutableListOf<Pair<Int, Int>>()

    fun attachService(service: Service) {
        this.service = service
    }

    fun detachService() {
        disable()
        this.service = null
    }

    fun enable() {
        val currentService = service ?: return
        if (gridView != null) return

        val windowManager =
            currentService.getSystemService(WINDOW_SERVICE) as WindowManager

        val grid = GridView(
            context = currentService,
            rows = DEFAULT_ROWS,
            cols = DEFAULT_COLS,
            lineOpacity = DEFAULT_LINE_OPACITY,
            labelOpacity = DEFAULT_LABEL_OPACITY,
            overlayOpacity = DEFAULT_OVERLAY_OPACITY,
            lineWidthPx = DEFAULT_LINE_WIDTH_PX,
            labelTextSizePx = DEFAULT_LABEL_TEXT_SIZE_PX
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
        val currentService = service ?: return
        val windowManager =
            currentService.getSystemService(WINDOW_SERVICE) as WindowManager
        gridView?.let { windowManager.removeView(it) }
        gridView = null
        selectedCell = null
        divisions.clear()
    }

    fun toggle() {
        if (gridView == null) enable() else disable()
    }

    private fun redraw() {
        gridView?.update(selectedCell, divisions.toList())
    }

    fun selectCell(cell: Int) {
        val maxCell = divisions.lastOrNull()?.second?.let { it * it }
            ?: ((gridView?.rows ?: DEFAULT_ROWS) * (gridView?.cols ?: DEFAULT_COLS))

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
            ?: ((gridView?.rows ?: DEFAULT_ROWS) * (gridView?.cols ?: DEFAULT_COLS))

        require(cell in 1..maxCell)

        if (selectedCell != cell) {
            divisions.clear()
        }

        selectedCell = cell
        divisions.add(cell to n)
        redraw()
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

    fun tapCell(cell: Int): Boolean {
        if (gridView == null) return false

        val screenPoint: ScreenPoint = getTouchCoords(cell)
        mediator.dispatchScoped(tapCommandFactory.create(screenPoint.x, screenPoint.y))
        return true
    }

    fun longTapCell(cell: Int, duration: Long = 1000): Boolean {
        if (gridView == null) return false

        val screenPoint: ScreenPoint = getTouchCoords(cell)
        mediator.dispatchScoped(longTapCommandFactory.create(screenPoint.x, screenPoint.y, duration))
        return true
    }

    fun swipeUpCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeUp(x, y, distance)
    }
    fun swipeDownCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeDown(x, y, distance)
    }
    fun swipeRightCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeRight(x, y, distance)
    }
    fun swipeLeftCell(cell: Int, distance: Float = 500f): Boolean {
        if (gridView == null) return false

        val (x, y) = getTouchCoords(cell)
        return touchExecutor.swipeLeft(x, y, distance)
    }

    fun pinchInCell(cell: Int, power: Float = 1f): Boolean {
        if (gridView == null) return false

        val screenPoint: ScreenPoint = getTouchCoords(cell)
        mediator.dispatchScoped(pinchInCommandFactory.create(screenPoint.x, screenPoint.y, power))
        return true
    }

    fun pinchOutCell(cell: Int, power: Float = 1f): Boolean {
        if (gridView == null) return false

        val screenPoint: ScreenPoint = getTouchCoords(cell)
        mediator.dispatchScoped(pinchOutCommandFactory.create(screenPoint.x, screenPoint.y, power))
        return true
    }
}
