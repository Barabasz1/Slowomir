package com.zpi.slowomir.overlay.ui.macro

import com.zpi.slowomir.overlay.macro.MacroInfo
import kotlin.text.removeSuffix

data class ActionEntry(
    val text: String
)

data class MacroEntry(
    val list: List<ActionEntry>,
    val isGenerated: Boolean = false
) {
    constructor(macroInfo: MacroInfo):
            this(
                macroInfo.commands
                    .map {
                        ActionEntry(it.javaClass.simpleName
                            .removeSuffix("Command")
                            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                        )
                },
                macroInfo.isGenerated)
}

