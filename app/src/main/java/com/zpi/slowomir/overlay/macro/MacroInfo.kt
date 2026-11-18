package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command

data class MacroInfo(
    val commands: List<Command> = emptyList(),
    val startingPoint: MacroStartingPoint? = null,
    val isGenerated: Boolean
)

fun MacroInfo.toMacro(): Macro {
    return Macro(
        commands = commands,
        startingPoint = startingPoint,
    )
}