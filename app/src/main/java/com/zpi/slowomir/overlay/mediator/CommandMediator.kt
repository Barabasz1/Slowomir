package com.zpi.slowomir.overlay.mediator

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.events.VoiceEvent
import com.zpi.slowomir.overlay.voice.OverlayState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


data class MediatorState(
    val overlayState: OverlayState = OverlayState.Idle,
    val overlayContext: OverlayContext = OverlayContext(
        packageName = "",
        isTextFieldFocused = false
    )
)


/**
 * Central Mediator orchestrating communication between event colleagues,
 * pipeline processing, context validation, overlay UI management, and command execution.
 */
interface CommandMediator {

    /**
     * Observable UI state representing the current session/voice state.
     */
    val mediatorState: StateFlow<MediatorState>

    /**
     * Reactive stream emitting execution results paired with their respective [Command].
     */
    val commandResults: SharedFlow<Pair<Command, CommandResult>>

    /**
     * Updates the current [OverlayContext].
     */
    fun updateContext(context: OverlayContext)

    /**
     * Receives and mediates incoming events from colleague modules (e.g. voice session).
     */
    suspend fun handleEvent(event: VoiceEvent)

    /**
     * Dispatches and executes a command after validating against [currentContext].
     *
     * @param command The [Command] to execute.
     * @return The resulting [CommandResult].
     */
    suspend fun dispatch(command: Command): CommandResult
}
