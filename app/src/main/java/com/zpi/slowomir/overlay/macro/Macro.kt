package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command

// Experimental macro class structure, feel free to change everything
data class Macro(
    val commands: List<Command> = emptyList(),
    val startingPoint: MacroStartingPoint? = null
)



