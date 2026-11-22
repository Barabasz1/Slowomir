package com.zpi.slowomir.overlay.executors

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.touch.LongTapCommand
import com.zpi.slowomir.overlay.commands.touch.PinchInCommand
import com.zpi.slowomir.overlay.commands.touch.PinchOutCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeDownCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeLeftCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeRightCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeUpCommand
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_HOLD_DURATION
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_PINCH_POWER
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_DISTANCE
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.ui.common.OverlayHostProvider
import com.zpi.slowomir.overlay.ui.common.OverlayWindowManager
import com.zpi.slowomir.overlay.ui.grid.GridView
import com.zpi.slowomir.utils.PreferencesManager
import com.zpi.slowomir.utils.ScreenPoint
import com.zpi.slowomir.voice.mediator.CommandMediator
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class GridExecutor @Inject constructor(
    hostProvider: OverlayHostProvider,
    private val mediator: CommandMediator,
    private val tapCommandFactory: TapCommand.Factory,
    private val longTapCommandFactory: LongTapCommand.Factory,
    private val swipeLeftCommandFactory: SwipeLeftCommand.Factory,
    private val swipeRightCommandFactory: SwipeRightCommand.Factory,
    private val swipeDownCommandFactory: SwipeDownCommand.Factory,
    private val swipeUpCommandFactory: SwipeUpCommand.Factory,
    private val pinchInCommandFactory: PinchInCommand.Factory,
    private val pinchOutCommandFactory: PinchOutCommand.Factory,
    private val swipeCommandFactory: SwipeCommand.Factory,
    private val preferencesManager: PreferencesManager,
) : OverlayWindowManager(hostProvider) {

    override val tag = "GridManager"


    private var gridView: GridView? = null
    private var parentCell: Int? = null
    private val divisions = mutableListOf<Pair<Int, Int>>()
    private var selectedCell: Int? = null
    private var rowsCount: Int = preferencesManager.gridRows.value
    private var colsCount: Int = preferencesManager.gridCols.value

    override fun onCreateView(context: Context): View {
        return GridView(
            context = context,
            rows = rowsCount,
            cols = colsCount,
            lineOpacity = 180,
            labelOpacity = 220,
            overlayOpacity = 0,
            lineWidthPx = 2f,
            labelTextSizePx = 32f,
            gridType = preferencesManager.gridType.value
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

    override fun toggle(): Boolean {
        if (isEnabled()) disable() else enable()
        return isEnabled()
    }

    fun isEnabled(): Boolean {
        return isShowing
    }

    fun isCellSelected(): Boolean {
        return selectedCell != null
    }

    fun canExecuteActionOnCell(): Boolean {
        return isCellSelected() && isEnabled()
    }

    fun enable() {
        if (isEnabled()) return
        resetSelection()
        show()
    }

    fun disable() {
        hide()
        gridView = null
        resetSelection()
    }

    fun resetDimensions() {
        rowsCount = preferencesManager.gridRows.value
        colsCount = preferencesManager.gridCols.value
    }

    fun resetSelection() {
        parentCell = null
        selectedCell = null
        divisions.clear()
    }

    fun redraw() {
        gridView?.update(
            rowsCount,
            colsCount,
            selectedCell,
            parentCell,
            divisions.toList(),
            preferencesManager.gridType.value
        )
    }

    fun addRow(toAdd: Int) {
        val oldRows = rowsCount
        val oldCols = colsCount
        val newRows = max(1, rowsCount + toAdd)
        if (newRows != oldRows) {
            rowsCount = newRows
            updateSelectionForNewDimensions(oldRows, oldCols, newRows, oldCols)
        }
        redraw()
    }

    fun addCols(toAdd: Int) {
        val oldRows = rowsCount
        val oldCols = colsCount
        val newCols = max(1, colsCount + toAdd)
        if (newCols != oldCols) {
            colsCount = newCols
            updateSelectionForNewDimensions(oldRows, oldCols, oldRows, newCols)
        }
        redraw()
    }

    private fun updateSelectionForNewDimensions(
        oldRows: Int,
        oldCols: Int,
        newRows: Int,
        newCols: Int
    ) {
        if (divisions.isEmpty()) {
            selectedCell?.let { cell ->
                val oldRow = (cell - 1) / oldCols
                val oldCol = (cell - 1) % oldCols
                val newRow = oldRow.coerceIn(0, newRows - 1)
                val newCol = oldCol.coerceIn(0, newCols - 1)
                selectedCell = newRow * newCols + newCol + 1
            }
        } else {
            parentCell?.let { cell ->
                val oldRow = (cell - 1) / oldCols
                val oldCol = (cell - 1) % oldCols
                val newRow = oldRow.coerceIn(0, newRows - 1)
                val newCol = oldCol.coerceIn(0, newCols - 1)
                val newParent = newRow * newCols + newCol + 1
                parentCell = newParent
                if (divisions.isNotEmpty()) {
                    divisions[0] = newParent to divisions[0].second
                }
            }
        }
    }

    fun selectCell(cell: Int) {
        val maxCell = if (divisions.isEmpty()) {
            (gridView?.cols ?: colsCount) * (gridView?.rows ?: rowsCount)
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
            ?: ((gridView?.rows ?: rowsCount) * (gridView?.cols ?: colsCount))

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

    fun undivide(): Boolean {
        divisions.removeLastOrNull()
        selectedCell = null
        redraw()
        return true
    }

    fun getCellBounds(cell: Int): RectF {
        val displayMetrics = runCatching {
            CommandExecutorService.instance?.resources?.displayMetrics
                ?: android.content.res.Resources.getSystem()?.displayMetrics
        }.getOrNull()

        val defaultW = displayMetrics?.widthPixels?.toFloat()?.takeIf { it > 0f } ?: 1080f
        val defaultH = displayMetrics?.heightPixels?.toFloat()?.takeIf { it > 0f } ?: 2400f

        val view = gridView
        var gridLeft = 0f
        var gridTop = 0f
        var width = defaultW
        var height = defaultH

        if (view != null && view.isAttachedToWindow && view.width > 0 && view.height > 0) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            gridLeft = location[0].toFloat()
            gridTop = location[1].toFloat()
            width = view.width.toFloat()
            height = view.height.toFloat()
        }

        val cols = view?.cols ?: colsCount
        val rows = view?.rows ?: rowsCount

        val maxCell = if (divisions.isEmpty()) {
            cols * rows
        } else {
            divisions.last().second * divisions.last().second
        }
        if (cell !in 1..maxCell) return RectF(0f, 0f, 0f, 0f)

        if (divisions.isEmpty()) {
            val cellIndex = cell - 1
            val row = cellIndex / cols
            val col = cellIndex % cols

            val cw = width / cols
            val ch = height / rows

            val left = gridLeft + col * cw
            val top = gridTop + row * ch
            return RectF(left, top, left + cw, top + ch)
        }

        val rootCell = divisions.first().first - 1
        val rootRow = rootCell / cols
        val rootCol = rootCell % cols

        var cellX = gridLeft + rootCol * (width / cols)
        var cellY = gridTop + rootRow * (height / rows)
        var cellW = width / cols
        var cellH = height / rows

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
        val left = cellX + targetCol * subW
        val top = cellY + targetRow * subH

        return RectF(left, top, left + subW, top + subH)
    }

    fun getCellCenter(cell: Int): ScreenPoint {
        val bounds = getCellBounds(cell)
        if (bounds.isEmpty) return ScreenPoint(0f, 0f)
        return ScreenPoint(bounds.centerX(), bounds.centerY())
    }

    fun getTouchCoords(cell: Int): ScreenPoint {
        return getCellCenter(cell)
    }

    private inline fun dispatchCellCommand(
        cell: Int,
        commandFactory: (x: Float, y: Float) -> Command
    ): Boolean {
        if (gridView == null) return false
        val (x, y) = getTouchCoords(cell)
        mediator.dispatchScoped(commandFactory(x, y))
        resetSelection()
        redraw()
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
        val (x0, y0) = getTouchCoords(cell0)
        val (x1, y1) = getTouchCoords(cell1)
        swipeCommandFactory.create(
            x0, y0,
            x1, y1
        )
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