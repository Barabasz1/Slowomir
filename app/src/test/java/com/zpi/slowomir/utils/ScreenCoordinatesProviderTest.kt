package com.zpi.slowomir.utils

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScreenCoordinatesProviderTest {

    private lateinit var provider: ScreenCoordinatesProvider

    @Before
    fun setUp() {
        provider = ScreenCoordinatesProvider()
    }

    @Test
    fun `default dimensions fall back to default screen dimensions`() {
        assertEquals(ScreenCoordinatesProvider.DEFAULT_SCREEN_WIDTH, provider.screenWidth, 0.01f)
        assertEquals(ScreenCoordinatesProvider.DEFAULT_SCREEN_HEIGHT, provider.screenHeight, 0.01f)
    }

    @Test
    fun `defaultTapCoordinates returns center tap defaults`() {
        val (x, y) = provider.defaultTapCoordinates
        assertEquals(ScreenCoordinatesProvider.DEFAULT_TAP_X, x, 0.01f)
        assertEquals(ScreenCoordinatesProvider.DEFAULT_TAP_Y, y, 0.01f)
    }

    @Test
    fun `getCoordinates computes fractions correctly`() {
        val (x, y) = provider.getCoordinates(0.5f, 0.5f)
        assertEquals(540f, x, 0.01f)
        assertEquals(1200f, y, 0.01f)

        val (originX, originY) = provider.getCoordinates(0f, 0f)
        assertEquals(0f, originX, 0.01f)
        assertEquals(0f, originY, 0.01f)
    }

    @Test
    fun `cellNumberToCoordinates computes center of cell 1`() {
        // columns = 4 (cellWidth = 270), rows = 5 (cellHeight = 480)
        // cell 1 -> col 0, row 0 -> (135, 240)
        val (x, y) = provider.cellNumberToCoordinates(1)
        assertEquals(135f, x, 0.01f)
        assertEquals(240f, y, 0.01f)
    }

    @Test
    fun `cellNumberToCoordinates computes center of cell 5`() {
        // cell 5 -> col 0, row 1 -> (135, 720)
        val (x, y) = provider.cellNumberToCoordinates(5)
        assertEquals(135f, x, 0.01f)
        assertEquals(720f, y, 0.01f)
    }

    @Test
    fun `cellNumberToCoordinates computes center of last cell 20`() {
        // cell 20 -> col 3, row 4 -> (945, 2160)
        val (x, y) = provider.cellNumberToCoordinates(20)
        assertEquals(945f, x, 0.01f)
        assertEquals(2160f, y, 0.01f)
    }
}
