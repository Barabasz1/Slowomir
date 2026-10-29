package com.zpi.slowomir.overlay.executors

import com.zpi.slowomir.overlay.ui.grid.GridService

/**
 * Executor responsible for toggling and controlling the grid overlay interface.
 *
 * @param gridService The service managing grid rendering and interaction.
 */
class GridExecutor(private val gridService: GridService) {

    /**
     * Toggles the visibility of the grid overlay.
     */
    fun toggle() {
        gridService.toggle()
    }
}