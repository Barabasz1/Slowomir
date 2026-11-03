package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridManager
import javax.inject.Inject

/**
 * Executor responsible for toggling and controlling the grid overlay interface.
 *
 * @param gridManager The service managing grid rendering and interaction.
 */
class GridExecutor @Inject constructor(private val gridManager: GridManager) {

    /**
     * Toggles the visibility of the grid overlay.
     */
    fun toggle() {
        gridManager.toggle()
    }

    /**
     * Divides the chosen cell by a certain degree.
     *
     * @param cell The cell to divide.
     * @param divisionPower The degree of division.
     */
    fun divide(cell: Int, divisionPower: Int) {
        gridManager.divide(cell, divisionPower)
    }
}