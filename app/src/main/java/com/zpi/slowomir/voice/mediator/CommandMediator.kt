package com.zpi.slowomir.voice.mediator

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.OverlayEvent
import com.zpi.slowomir.voice.VoiceIntent
import kotlinx.coroutines.flow.StateFlow


data class MediatorState(
    val overlayState: OverlayState = OverlayState.Idle,
    val overlayContext: OverlayContext = OverlayContext(
        packageName = "",
        isTextFieldFocused = false
    ),
    val intent: VoiceIntent? = null,
    /**
     * Contacts waiting for the user's choice. Non-null means the contact picker
     * should be visible and speech is interpreted only as a selection or cancel.
     */
    val contactSelection: List<ContactOption>? = null,
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
     * Updates the current [OverlayContext].
     */
    fun updateContext(context: OverlayContext)

    /**
     * Receives and mediates incoming events from colleague modules (e.g. voice session).
     */
    suspend fun handleEvent(event: OverlayEvent)

    /**
     * Dispatches and executes a command after validating against [currentContext].
     *
     * @param command The [Command] to execute.
     * @return The resulting [CommandResult].
     */
    suspend fun dispatch(command: Command): CommandResult
}