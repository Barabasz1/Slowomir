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
import com.zpi.slowomir.utils.ScreenCoordinatesProvider
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
    private val screenCoordinatesProvider: ScreenCoordinatesProvider,
) {

    fun resolve(intent: VoiceIntent): Command = when (intent.identifier) {
        // Navigation
        CommandIdentifier.CELL_PRESS -> {
            val (x, y) = when (val arg = intent.argument) {
                is CommandArgument.Number -> screenCoordinatesProvider.cellNumberToCoordinates(arg.value)
                else -> screenCoordinatesProvider.defaultTapCoordinates
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

    /** Builds a command at a point given as fractions of screen width/height. */
    private fun <T : Command> withScreenPoint(
        xFraction: Float,
        yFraction: Float,
        create: (Float, Float) -> T,
    ): T {
        val (x, y) = screenCoordinatesProvider.getCoordinates(xFraction, yFraction)
        return create(x, y)
    }
}
