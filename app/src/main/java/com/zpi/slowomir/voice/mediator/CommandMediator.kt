package com.zpi.slowomir.voice.mediator

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.AppItem
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
    /**
     * List of applications waiting for the user's choice,
     * if the app name provided by user is ambiguous.
     */
    val appSelection: List<AppItem>? = null,
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
     * Sets or clears the active contact selection options.
     */
    fun setContactSelection(options: List<ContactOption>?)

    /**
     * Sets or clears the active app launcher selection options.
     */
    fun setAppSelection(options: List<AppItem>?)

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