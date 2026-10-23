package com.zpi.slowomir.overlay.mediator

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central mediator interfacing command emitters, pipelines, context state, and command execution logic.
 * Decouples producers of commands from the command execution runtime.
 */
interface CommandMediator {

    /**
     * Reactive stream of dispatched commands observed across the application.
     */
    val commandStream: SharedFlow<Command>

    /**
     * Reactive stream emitting execution results paired with their respective [Command].
     */
    val commandResults: SharedFlow<Pair<Command, CommandResult>>

    /**
     * Current [OverlayContext] state used to validate command prerequisites.
     */
    val currentContext: StateFlow<OverlayContext>

    /**
     * Updates the current [OverlayContext].
     */
    fun updateContext(context: OverlayContext)

    /**
     * Dispatches and executes a command after validating against [currentContext].
     *
     * @param command The [Command] to execute.
     * @return The resulting [CommandResult].
     */
    suspend fun dispatch(command: Command): CommandResult
}
