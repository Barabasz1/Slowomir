package com.zpi.slowomir.overlay.services.grid

import android.app.Service
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager

class GridService(private val service: Service) {
    private val windowManager: WindowManager = service.getSystemService(WINDOW_SERVICE) as WindowManager
    private var gridView: GridView? = null

    fun enable(){
        val grid = GridView(
            context = service,
            rows = 10,
            cols = 6,
            lineOpacity = 180,
            labelOpacity = 220,
            overlayOpacity = 0,
            lineWidthPx = 2f,
            labelTextSizePx = 32f
        )
        val windowType =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
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
            x = 0
            y = 0
        }
        windowManager.addView(grid, params)
        gridView = grid
    }

    fun disable(){
        gridView?.let { windowManager.removeView(it) }
        gridView = null
    }

    fun toggle(){
        if(gridView == null) {
            enable()
        } else {
            disable()
        }
    }
}
