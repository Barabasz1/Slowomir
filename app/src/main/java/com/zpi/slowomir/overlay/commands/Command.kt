package com.zpi.slowomir.overlay.commands

import com.zpi.slowomir.overlay.OverlayContext

/**
 * Represents an executable action designed to be run within the context of the [com.zpi.slowomir.overlay.services.CommandExecutorService].
 */
interface Command {
    /**
     * Executes the encapsulated logic and returns the outcome.
     *
     * @return A [CommandResult] indicating whether the execution succeeded, failed, or encountered an error.
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

