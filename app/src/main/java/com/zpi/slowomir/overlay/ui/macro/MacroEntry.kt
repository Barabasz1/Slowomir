package com.zpi.slowomir.overlay.ui.macro

import android.content.Context
import android.content.pm.PackageManager
import com.zpi.slowomir.overlay.macro.Macro

data class ActionEntry(
    val text: String
)

data class MacroEntry(
    val list: List<ActionEntry>,
    val isGenerated: Boolean = false
)


fun MacroEntry(macro: Macro, appLabel: (String) -> String? = { null }): MacroEntry {

    val stored = macro.serializedCommands
    val texts = if (stored.isNotEmpty() && stored.size == macro.commands.size) {
        stored.map { MacroCommandFormatter.format(it.type.substringAfterLast('.'), it.arguments, appLabel) }
    } else {
        macro.commands.map { MacroCommandFormatter.format(it, appLabel) }
    }
    return MacroEntry(
        list = (listOfNotNull(MacroCommandFormatter.formatStartingPoint(macro.startingPoint, appLabel)) + texts)
            .map { ActionEntry(it) },
        isGenerated = macro.isGenerated
    )
}

fun appLabelResolver(context: Context): (String) -> String? {
    val packageManager = context.packageManager
    return { packageName ->
        runCatching {
            @Suppress("DEPRECATION")
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
    }
}
