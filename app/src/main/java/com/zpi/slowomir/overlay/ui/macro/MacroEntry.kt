package com.zpi.slowomir.overlay.ui.macro

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.macro.Macro
import java.util.Locale

data class ActionEntry(
    val text: String
)

data class MacroEntry(
    val list: List<ActionEntry>,
    val isGenerated: Boolean = false
)

fun MacroEntry(macro: Macro): MacroEntry {
    return MacroEntry(
        list = macro.commands.map { ActionEntry(formatCommand(it)) },
        isGenerated = macro.isGenerated
    )
}

private fun formatCommand(command: Command): String {
    val baseName = command.javaClass.simpleName
        .removeSuffix("Command")
        .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    val args = command.arguments.filterValues { it != null }
    if (args.isEmpty()) return baseName
    val formattedArgs = args.values.joinToString(", ") { value ->
        when (value) {
            is Float -> if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(Locale.US, value)
            else -> value.toString()
        }
    }
    return "$baseName ($formattedArgs)"
}
