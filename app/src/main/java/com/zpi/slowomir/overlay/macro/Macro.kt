package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.SerializedCommand

data class Macro(
    val commands: List<Command> = emptyList(),
    val startingPoint: MacroStartingPoint? = null,
    val isGenerated: Boolean = false,
    val serializedCommands: List<SerializedCommand> = emptyList(),
)