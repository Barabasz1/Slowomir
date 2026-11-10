package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.*
import com.zpi.slowomir.overlay.commands.phoneCalls.*
import com.zpi.slowomir.overlay.commands.grid.*
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.utils.ScreenCoordinatesProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandResolver @Inject constructor(
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
    private val swipeUpNavCommandFactory: SwipeUpNavCommand.Factory,
    private val swipeDownNavCommandFactory: SwipeDownNavCommand.Factory,
    private val swipeLeftNavCommandFactory: SwipeLeftNavCommand.Factory,
    private val swipeRightNavCommandFactory: SwipeRightNavCommand.Factory,
    private val zoomInNavCommandFactory: ZoomInNavCommand.Factory,
    private val zoomOutNavCommandFactory: ZoomOutNavCommand.Factory,
    private val volumeUpCommandFactory: VolumeUpCommand.Factory,
    private val volumeDownCommandFactory: VolumeDownCommand.Factory,
    private val openAppCommandFactory: OpenAppCommand.Factory,
    private val enableGridCommandFactory: EnableGridCommand.Factory,
    private val disableGridCommandFactory: EnableGridCommand.Factory,
    private val resetGridCommandFactory: ResetGridCommand.Factory,
    private val selectCellCommandFactory: SelectCellCommand.Factory,
    private val divideCellCommandFactory: DivideCellCommand.Factory,
    private val undivideCellCommandFactory: UndivideCellCommand.Factory,
    private val tapCellCommandFactory: TapCellCommand.Factory,
    private val longTapCellCommandFactory: LongTapCellCommand.Factory,
    private val screenCoordinatesProvider: ScreenCoordinatesProvider,
) {

    fun resolve(intent: VoiceIntent): Command = when (intent.identifier) {
        // Navigation

        CommandIdentifier.NOTIFICATIONS -> notificationsCommandFactory.create()
        CommandIdentifier.QUICK_SETTINGS -> quickSettingsCommandFactory.create()
        CommandIdentifier.LOCK_SCREEN -> lockScreenCommandFactory.create()
        CommandIdentifier.SCREENSHOT -> screenshotCommandFactory.create()
        CommandIdentifier.NAVIGATE_HOME -> navigateHomeCommandFactory.create()
        CommandIdentifier.NAVIGATE_RECENT -> navigateRecentCommandFactory.create()
        CommandIdentifier.NAVIGATE_BACK -> navigateBackCommandFactory.create()
        // NAVIGATE_APP_OPEN
        CommandIdentifier.NAVIGATE_APP_OPEN -> openAppCommandFactory.create(intent.textArgument())


        // Grid

        //TODO maybe add GRID_TOGGLE command? just "siatka"
        CommandIdentifier.GRID_SHOW -> enableGridCommandFactory.create()
        CommandIdentifier.GRID_HIDE -> disableGridCommandFactory.create()
        CommandIdentifier.GRID_RESET -> resetGridCommandFactory.create()
        CommandIdentifier.CELL_SELECT -> {
            val arg = intent.intArgument()
            if (arg != null)
                selectCellCommandFactory.create(arg)
            else
                voidCommand(intent)
        }

        CommandIdentifier.CELL_DIVIDE -> divideCellCommandFactory.create()
        CommandIdentifier.CELL_UNDIVIDE -> undivideCellCommandFactory.create()
        CommandIdentifier.CELL_PRESS -> tapCellCommandFactory.create()
        CommandIdentifier.CELL_LONG_PRESS -> longTapCellCommandFactory.create()

        //rest of cell actions are shared with navigation commands because they are ambiguous

        // Gestures
        CommandIdentifier.SWIPE_UP -> swipeUpNavCommandFactory.create()
        CommandIdentifier.SWIPE_DOWN -> swipeDownNavCommandFactory.create()
        CommandIdentifier.SWIPE_LEFT -> swipeLeftNavCommandFactory.create()
        CommandIdentifier.SWIPE_RIGHT -> swipeRightNavCommandFactory.create()
        CommandIdentifier.ZOOM_IN -> zoomInNavCommandFactory.create()
        CommandIdentifier.ZOOM_OUT -> zoomOutNavCommandFactory.create()

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

        else -> LogCommand(intent.identifier, intent.argument)
    }


    private fun VoiceIntent.textArgument(): String? =
        (argument as? CommandArgument.Text)?.value

    private fun VoiceIntent.intArgument(): Int? =
        (argument as? CommandArgument.Number)?.value

    // fallback when a blank command is needed (user did not provide an argument for example)
    private fun voidCommand(intent: VoiceIntent): Command {
        return LogCommand(intent.identifier, intent.argument)
    }
}
