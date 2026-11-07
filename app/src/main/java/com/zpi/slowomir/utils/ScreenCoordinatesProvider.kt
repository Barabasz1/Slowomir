package com.zpi.slowomir.utils

import android.content.res.Resources
import android.util.DisplayMetrics
import com.zpi.slowomir.overlay.services.CommandExecutorService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the coordinates of the screen.
 */
@Singleton
class ScreenCoordinatesProvider @Inject constructor() {
    val displayMetrics: DisplayMetrics?
        get() = CommandExecutorService.instance?.resources?.displayMetrics
            ?: Resources.getSystem().displayMetrics
    val screenWidth: Float
        get() = displayMetrics?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH
    val screenHeight: Float
        get() = displayMetrics?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT

    /**
     * Returns the coordinates of the screen.
     *
     * @param normalizedX The normalizer for the width.
     * @param normalizedY The normalizer for the height.
     * @return The coordinates of the screen.
     */
    fun getCoordinates(normalizedX: Float, normalizedY: Float): Pair<Float, Float> {
        return Pair(screenWidth * normalizedX, screenHeight * normalizedY)
    }

    /**
     * Dynamic mapping from a cell number to screen coordinates based on active screen metrics.
     * Assumes a 4-column, 5-row grid covering the full screen area.
     *
     * @param cellNumber The 1-based cell number to map.
     * @return A pair of (x, y) coordinates representing the center of the cell.
     */
     fun cellNumberToCoordinates(cellNumber: Int): Pair<Float, Float> {
        val columns = 4
        val rows = 5
        val cellWidth = screenWidth / columns
        val cellHeight = screenHeight / rows

        val zeroIndexed = (cellNumber - 1).coerceAtLeast(0)
        val col = zeroIndexed % columns
        val row = zeroIndexed / columns

        val x = col * cellWidth + cellWidth / 2f
        val y = row * cellHeight + cellHeight / 2f
        return Pair(x.coerceIn(0f, screenWidth), y.coerceIn(0f, screenHeight))
    }

    companion object {
        private const val DEFAULT_SCREEN_WIDTH = 1080f
        private const val DEFAULT_SCREEN_HEIGHT = 2400f
    }
}
