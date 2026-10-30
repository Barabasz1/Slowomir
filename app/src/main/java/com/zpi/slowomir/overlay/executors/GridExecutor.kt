package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridManager


class GridExecutor(private val gridManager: GridManager) {

    fun toggle(){
        gridManager.toggle()
    }

    fun divide(cell: Int, divisionPower: Int){
        gridManager.divide(cell, divisionPower)
    }
}