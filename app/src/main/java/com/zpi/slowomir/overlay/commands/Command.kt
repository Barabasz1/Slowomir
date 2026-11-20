package com.zpi.slowomir.overlay.commands

import com.zpi.slowomir.overlay.OverlayContext
import java.util.Locale
/**
 * Represents an executable action designed to be run within the context of the [com.zpi.slowomir.overlay.services.CommandExecutorService].
 */
interface Command {
    /**
     * Requests execution off the main thread when confirming asynchronous platform effects.
     */
    val requiresBackgroundExecution: Boolean
        get() = false

    val commandType: String
        get() = this::class.java.name

    val arguments: Map<String, Any?>
        get() = emptyMap()

    fun formatDisplay(): String {
        val baseName = this::class.java.simpleName
            .removeSuffix("Command")
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        val filtered = arguments.filterValues { it != null }
        if (filtered.isEmpty()) return baseName
        val argsText = filtered.entries.joinToString(", ") { (k, v) ->
            val formattedVal = when (v) {
                is Float -> if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(Locale.US, v)
                else -> v.toString()
            }
            "$k=$formattedVal"
        }
        return "$baseName($argsText)"
    }
    /**
     * Executes the encapsulated logic and returns the outcome.
     *
     * @return A [CommandResult] indicating whether the execution succeeded, failed or encountered an error.
     */
    fun execute(): CommandResult

    /**
     * Evaluates whether all prerequisites required for executing this command are satisfied.
     *
     * @param context The current [OverlayContext] containing state details of the display and service.
     * @return `true` if the command is valid for execution under the current conditions, `false` otherwise.
     */
    fun canExecute(context: OverlayContext): Boolean
}

