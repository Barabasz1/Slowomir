package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.*
import com.zpi.slowomir.overlay.commands.phoneCalls.*
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.overlay.services.CommandExecutorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandResolver @Inject constructor(
    private val tapCommandFactory: TapCommand.Factory,
    private val notificationsCommandFactory: NotificationsCommand.Factory,
    private val quickSettingsCommandFactory: QuickSettingsCommand.Factory,
    private val lockScreenCommandFactory: LockScreenCommand.Factory,
    private val screenshotCommandFactory: ScreenshotCommand.Factory,
    private val navigateHomeCommandFactory: NavigateHomeCommand.Factory,
    private val navigateRecentCommandFactory: NavigateRecentCommand.Factory,
    private val navigateBackCommandFactory: NavigateBackCommand.Factory,
    private val keyboardHideCommandFactory: KeyboardHideCommand.Factory,
    private val keyboardShowCommandFactory: KeyboardShowCommand.Factory,
    private val pickUpCommandFactory: PickUpCommand.Factory,
    private val declineCommandFactory: DeclineCommand.Factory,
    private val endCommandFactory: EndCommand.Factory,
    private val speakerCommandFactory: SpeakerCommand.Factory,
    private val callCommandFactory: CallCommand.Factory,
    private val writeSMSCommandFactory: WriteSMSCommand.Factory,
    private val swipeUpCommandFactory: SwipeUpCommand.Factory,
    private val swipeDownCommandFactory: SwipeDownCommand.Factory,
    private val swipeLeftCommandFactory: SwipeLeftCommand.Factory,
    private val swipeRightCommandFactory: SwipeRightCommand.Factory,
    private val zoomInCommandFactory: ZoomInCommand.Factory,
    private val zoomOutCommandFactory: ZoomOutCommand.Factory,
    private val volumeUpCommandFactory: VolumeUpCommand.Factory,
    private val volumeDownCommandFactory: VolumeDownCommand.Factory,
) {

    fun resolve(intent: VoiceIntent): Command = when (intent.identifier) {
        // Navigation
        CommandIdentifier.CELL_PRESS -> {
            val (x, y) = when (val arg = intent.argument) {
                is CommandArgument.Number -> cellNumberToCoordinates(arg.value)
                else -> DEFAULT_TAP_X to DEFAULT_TAP_Y
            }
            tapCommandFactory.create(x, y)
        }
        CommandIdentifier.NOTIFICATIONS -> notificationsCommandFactory.create()
        CommandIdentifier.QUICK_SETTINGS -> quickSettingsCommandFactory.create()
        CommandIdentifier.LOCK_SCREEN -> lockScreenCommandFactory.create()
        CommandIdentifier.SCREENSHOT -> screenshotCommandFactory.create()
        CommandIdentifier.NAVIGATE_HOME -> navigateHomeCommandFactory.create()
        CommandIdentifier.NAVIGATE_RECENT -> navigateRecentCommandFactory.create()
        CommandIdentifier.NAVIGATE_BACK -> navigateBackCommandFactory.create()

        // Gestures
        CommandIdentifier.SWIPE_UP -> withScreenPoint(0.5f, 0.8f, swipeUpCommandFactory::create)
        CommandIdentifier.SWIPE_DOWN -> withScreenPoint(0.5f, 0.2f, swipeDownCommandFactory::create)
        CommandIdentifier.SWIPE_LEFT -> withScreenPoint(0.8f, 0.5f, swipeLeftCommandFactory::create)
        CommandIdentifier.SWIPE_RIGHT -> withScreenPoint(0.2f, 0.5f, swipeRightCommandFactory::create)
        CommandIdentifier.ZOOM_IN -> withScreenPoint(0.5f, 0.5f, zoomInCommandFactory::create)
        CommandIdentifier.ZOOM_OUT -> withScreenPoint(0.5f, 0.5f, zoomOutCommandFactory::create)

        // Text editing
        CommandIdentifier.HIDE_KEYBOARD -> keyboardHideCommandFactory.create()
        CommandIdentifier.SHOW_KEYBOARD -> keyboardShowCommandFactory.create()

        // Phone calls
        CommandIdentifier.PHONE_PICK_UP -> pickUpCommandFactory.create()
        CommandIdentifier.PHONE_DECLINE -> declineCommandFactory.create()
        CommandIdentifier.PHONE_END_CALL -> endCommandFactory.create()
        CommandIdentifier.PHONE_SPEAKER -> speakerCommandFactory.create()
        CommandIdentifier.PHONE_CALL ->
            callCommandFactory.create(recipient = intent.textArgument())
        CommandIdentifier.SMS_WRITE ->
            writeSMSCommandFactory.create(recipient = intent.textArgument())

        // Media
        CommandIdentifier.VOLUME_UP -> volumeUpCommandFactory.create()
        CommandIdentifier.VOLUME_DOWN -> volumeDownCommandFactory.create()

        // NAVIGATE_APP_OPEN
        else -> LogCommand(intent.identifier, intent.argument)
    }

    /** Used when the user picks a contact from the selection list. */
    fun createCall(recipient: String?): Command =
        callCommandFactory.create(recipient = recipient)

    /** Used when the user picks a contact from the selection list for SMS writing. */
    fun createWriteSMS(recipient: String?): Command =
        writeSMSCommandFactory.create(recipient = recipient)

    // ---- helpers ----

    private fun VoiceIntent.textArgument(): String? =
        (argument as? CommandArgument.Text)?.value

    private fun screenSize(): Pair<Float, Float> {
        val m = CommandExecutorService.instance?.resources?.displayMetrics
        return (m?.widthPixels?.toFloat() ?: DEFAULT_SCREEN_WIDTH) to
                (m?.heightPixels?.toFloat() ?: DEFAULT_SCREEN_HEIGHT)
    }

    /** Builds a command at a point given as fractions of screen width/height. */
    private fun <T : Command> withScreenPoint(
        xFraction: Float,
        yFraction: Float,
        create: (Float, Float) -> T,
    ): T {
        val (w, h) = screenSize()
        return create(w * xFraction, h * yFraction)
    }

    /**
     * Dynamic mapping from a cell number to screen coordinates based on active screen metrics.
     * Assumes a 4-column, 5-row grid covering the full screen area.
     *
     * @param cellNumber The 1-based cell number to map.
     * @return A pair of (x, y) coordinates representing the center of the cell.
     */
    private fun cellNumberToCoordinates(cellNumber: Int): Pair<Float, Float> {
        val (screenWidth, screenHeight) = screenSize()

        val columns = 4
        val rows = 5
        val cellWidth = screenWidth / columns
        val cellHeight = screenHeight / rows

        val zeroIndexed = (cellNumber - 1).coerceAtLeast(0)
        val col = zeroIndexed % columns
        val row = zeroIndexed / columns

        val x = col * cellWidth + cellWidth / 2f
        val y = row * cellHeight + cellHeight / 2f
        return Pair(x.coerceIn(0f, screenWidth), y.coerceIn(0f, screenHeight))
    }

    private companion object {
        const val DEFAULT_SCREEN_WIDTH = 1080f
        const val DEFAULT_SCREEN_HEIGHT = 2400f
        const val DEFAULT_TAP_X = 540f
        const val DEFAULT_TAP_Y = 1200f
    }
}