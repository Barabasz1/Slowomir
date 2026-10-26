package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.services.grid.GridService


class GridExecutor(private val gridService: GridService) {

    fun toggle(){
        gridService.toggle()
    }
}