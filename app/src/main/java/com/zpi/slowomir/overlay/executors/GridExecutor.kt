package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridExecutor @Inject constructor(private val gridManager: GridManager) {

    fun toggle() {
        gridManager.toggle()
    }

    /**
     * Divides the chosen cell by a certain degree.
     *
     * @param cell The cell to divide.
     * @param divisionPower The degree of division.
     */
    fun divide(cell: Int, divisionPower: Int){
        gridManager.divide(cell, divisionPower)
    }
}