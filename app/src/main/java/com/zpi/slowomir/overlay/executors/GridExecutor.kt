package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridManager

/**
 * Executor responsible for toggling and controlling the grid overlay interface.
 *
 * @param gridService The service managing grid rendering and interaction.
 */
class GridExecutor(private val gridManager: GridManager) {

    /**
     * Toggles the visibility of the grid overlay.
     */
    fun toggle(){
        gridManager.toggle()
    }

    /**
     * Divides the chosen cell by a certain degree.
     *
     * @param cell
     * @param divisionPower
     */
    fun divide(cell: Int, divisionPower: Int){
        gridManager.divide(cell, divisionPower)
    }
}