package com.zpi.slowomir.overlay.macro

sealed class MacroStartingPoint {
    object Home : MacroStartingPoint()
    data class App(val appName: String) : MacroStartingPoint()
}

