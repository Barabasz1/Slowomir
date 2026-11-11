package com.zpi.slowomir.overlay.macro

// Experimental macro class structure, feel free to change everything
sealed class MacroStartingPoint {
    object Home : MacroStartingPoint()
    data class App(val appName: String) : MacroStartingPoint()
}

