package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.overlay.LabelsToggleCommand
import com.zpi.slowomir.overlay.commands.overlay.SelectLabelCommand
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.executors.LabelsExecutor
import javax.inject.Provider
import com.zpi.slowomir.overlay.commands.overlay.SleepCommand
import com.zpi.slowomir.overlay.commands.media.NextTrackCommand
import com.zpi.slowomir.overlay.commands.media.PauseCommand
import com.zpi.slowomir.overlay.commands.media.PlayCommand
import com.zpi.slowomir.overlay.commands.media.PreviousTrackCommand
import com.zpi.slowomir.overlay.commands.media.SkipBackwardCommand
import com.zpi.slowomir.overlay.commands.media.SkipForwardCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.*
import com.zpi.slowomir.overlay.commands.phoneCalls.*
import com.zpi.slowomir.overlay.commands.predictions.ConfirmCommand as PredictionConfirmCommand
import com.zpi.slowomir.overlay.commands.predictions.DismissCommand as PredictionDismissCommand
import com.zpi.slowomir.overlay.commands.grid.*
import com.zpi.slowomir.overlay.commands.textEdit.CancelCommand
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_FORCE
import com.zpi.slowomir.overlay.commands.textEdit.EditCommand
import com.zpi.slowomir.overlay.commands.textEdit.ConfirmCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorBackwardCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorForwardCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorWordNextCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorWordPreviousCommand
import com.zpi.slowomir.overlay.commands.textEdit.DeleteCharCommand
import com.zpi.slowomir.overlay.commands.textEdit.DeleteWordCommand
import com.zpi.slowomir.overlay.commands.textEdit.EnterCommand
import com.zpi.slowomir.overlay.commands.textEdit.PunctuationCommand
import com.zpi.slowomir.overlay.commands.macro.DeleteLastRecordedActionCommand
import com.zpi.slowomir.overlay.commands.macro.DeleteMacroCommand
import com.zpi.slowomir.overlay.commands.macro.KeepMacroCommand
import com.zpi.slowomir.overlay.commands.macro.FinishMacroCommand
import com.zpi.slowomir.voice.intent.CommandIntentParser
import com.zpi.slowomir.overlay.commands.macro.HideMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.macro.PlayMacroCommand
import com.zpi.slowomir.overlay.commands.macro.RecordMacroCommand
import com.zpi.slowomir.overlay.commands.macro.ShowMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.macro.ToggleMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardToggleCommand
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.MAX_SWIPE_FORCE
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
    private val navigateAppLibraryCommandFactory: NavigateAppLibraryCommand.Factory,
    private val navigateBackCommandFactory: NavigateBackCommand.Factory,
    private val keyboardHideCommandFactory: KeyboardHideCommand.Factory,
    private val keyboardShowCommandFactory: KeyboardShowCommand.Factory,
    private val keyboardToggleCommandFactory: KeyboardToggleCommand.Factory,
    private val textCursorForwardCommandFactory: CursorForwardCommand.Factory,
    private val textCursorBackwardCommandFactory: CursorBackwardCommand.Factory,
    private val textCursorWordNextCommandFactory: CursorWordNextCommand.Factory,
    private val textCursorWordPreviousCommandFactory: CursorWordPreviousCommand.Factory,
    private val textDeleteCharCommandFactory: DeleteCharCommand.Factory,
    private val textDeleteWordCommandFactory: DeleteWordCommand.Factory,
    private val textEnterCommandFactory: EnterCommand.Factory,
    private val textConfirmCommandFactory: ConfirmCommand.Factory,
    private val textCancelCommandFactory: CancelCommand.Factory,
    private val textEditCommandFactory: EditCommand.Factory,
    private val textPunctuationCommandFactory: PunctuationCommand.Factory,
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
    private val playCommandFactory: PlayCommand.Factory,
    private val pauseCommandFactory: PauseCommand.Factory,
    private val skipForwardCommandFactory: SkipForwardCommand.Factory,
    private val skipBackwardCommandFactory: SkipBackwardCommand.Factory,
    private val nextTrackCommandFactory: NextTrackCommand.Factory,
    private val previousTrackCommandFactory: PreviousTrackCommand.Factory,
    private val openAppCommandFactory: OpenAppCommand.Factory,
    private val gridEnableCommandFactory: GridEnableCommand.Factory,
    private val disableGridCommandFactory: GridDisableCommand.Factory,
    private val gridToggleCommandFactory: GridToggleCommand.Factory,
    private val labelsToggleCommandFactory: LabelsToggleCommand.Factory,
    private val selectLabelCommandFactory: SelectLabelCommand.Factory,
    private val labelsExecutorProvider: Provider<LabelsExecutor?>,
    private val gridExecutorProvider: Provider<GridExecutor?>,
    private val gridResetCommandFactory: GridResetCommand.Factory,
    private val addColumnGridCommandFactory: AddColumnGridCommand.Factory,
    private val addRowGridCommandFactory: AddRowGridCommand.Factory,
    private val subtractColumnGridCommandFactory: SubtractColumnGridCommand.Factory,
    private val subtractRowGridCommandFactory: SubtractRowGridCommand.Factory,
    private val selectCellCommandFactory: SelectCellCommand.Factory,
    private val divideCellCommandFactory: DivideCellCommand.Factory,
    private val undivideCellCommandFactory: UndivideCellCommand.Factory,
    private val tapCellCommandFactory: TapCellCommand.Factory,
    private val longTapCellCommandFactory: LongTapCellCommand.Factory,
    private val showMacroOverlayCommandFactory: ShowMacroOverlayCommand.Factory,
    private val hideMacroOverlayCommandFactory: HideMacroOverlayCommand.Factory,
    private val toggleMacroOverlayCommandFactory: ToggleMacroOverlayCommand.Factory,
    private val playMacroCommandFactory: PlayMacroCommand.Factory,
    private val recordMacroCommandFactory: RecordMacroCommand.Factory,
    private val finishMacroCommandFactory: FinishMacroCommand.Factory,
    private val deleteMacroCommandFactory: DeleteMacroCommand.Factory,
    private val keepMacroCommandFactory: KeepMacroCommand.Factory,
    private val deleteLastRecordedActionCommandFactory: DeleteLastRecordedActionCommand.Factory,
    private val sleepCommandFactory: SleepCommand.Factory,
    private val predictionConfirmCommandFactory: PredictionConfirmCommand.Factory,
    private val predictionDismissCommandFactory: PredictionDismissCommand.Factory,
) {

    fun resolve(intent: VoiceIntent): Command = when (intent.identifier) {
        // Navigation
        CommandIdentifier.NOTIFICATIONS -> notificationsCommandFactory.create()
        CommandIdentifier.QUICK_SETTINGS -> quickSettingsCommandFactory.create()
        CommandIdentifier.LOCK_SCREEN -> lockScreenCommandFactory.create()
        CommandIdentifier.SCREENSHOT -> screenshotCommandFactory.create()
        CommandIdentifier.NAVIGATE_HOME -> navigateHomeCommandFactory.create()
        CommandIdentifier.NAVIGATE_RECENT -> navigateRecentCommandFactory.create()
        CommandIdentifier.NAVIGATE_APP_LIBRARY -> navigateAppLibraryCommandFactory.create()
        CommandIdentifier.NAVIGATE_BACK -> navigateBackCommandFactory.create()
        // NAVIGATE_APP_OPEN
        CommandIdentifier.NAVIGATE_APP_OPEN -> openAppCommandFactory.create(intent.textArgument())

        // Grid
        CommandIdentifier.GRID_TOGGLE -> gridToggleCommandFactory.create()
        CommandIdentifier.LABELS_TOGGLE -> labelsToggleCommandFactory.create()
        CommandIdentifier.GRID_SHOW -> gridEnableCommandFactory.create()
        CommandIdentifier.GRID_HIDE -> disableGridCommandFactory.create()
        CommandIdentifier.GRID_RESET -> gridResetCommandFactory.create()
        CommandIdentifier.GRID_ADD_COL -> addColumnGridCommandFactory.create()
        CommandIdentifier.GRID_ADD_ROW -> addRowGridCommandFactory.create()
        CommandIdentifier.GRID_SUBTRACT_COL -> subtractColumnGridCommandFactory.create()
        CommandIdentifier.GRID_SUBTRACT_ROW -> subtractRowGridCommandFactory.create()

        CommandIdentifier.CELL_SELECT -> {
            val arg = intent.intArgument()
            when {
                arg != null && labelsAddressed() -> selectLabelCommandFactory.create(arg, null, false)
                arg != null -> selectCellCommandFactory.create(arg)
                else -> voidCommand(intent)
            }
        }

        CommandIdentifier.CELL_DIVIDE -> {
            val arg = intent.intArgument()
            val pow =
                if (arg != null && arg == 9)
                    3
                else
                    2

            divideCellCommandFactory.create(pow)
        }

        CommandIdentifier.CELL_UNDIVIDE -> undivideCellCommandFactory.create()
        CommandIdentifier.CELL_PRESS ->
            intent.intArgument()?.takeIf { labelsAddressed() }
                ?.let { selectLabelCommandFactory.create(it, null, false) }
                ?: tapCellCommandFactory.create()
        CommandIdentifier.CELL_LONG_PRESS ->
            intent.intArgument()?.takeIf { labelsAddressed() }
                ?.let { selectLabelCommandFactory.create(it, null, true) }
                ?: longTapCellCommandFactory.create()
        CommandIdentifier.LABEL_PRESS -> selectLabelCommandFactory.create(null, intent.textArgument(), false)
        CommandIdentifier.LABEL_LONG_PRESS -> selectLabelCommandFactory.create(null, intent.textArgument(), true)

        //rest of cell actions are shared with navigation commands because they are ambiguous

        // Gestures
        CommandIdentifier.SWIPE_UP -> swipeUpNavCommandFactory.create(intent.swipeForce())
        CommandIdentifier.SWIPE_DOWN -> swipeDownNavCommandFactory.create(intent.swipeForce())
        CommandIdentifier.SWIPE_LEFT -> swipeLeftNavCommandFactory.create(intent.swipeForce())
        CommandIdentifier.SWIPE_RIGHT -> swipeRightNavCommandFactory.create(intent.swipeForce())
        CommandIdentifier.ZOOM_IN -> zoomInNavCommandFactory.create()
        CommandIdentifier.ZOOM_OUT -> zoomOutNavCommandFactory.create()

        // Text editing
        CommandIdentifier.KEYBOARD_HIDE -> keyboardHideCommandFactory.create()
        CommandIdentifier.KEYBOARD_SHOW -> keyboardShowCommandFactory.create()
        CommandIdentifier.KEYBOARD_TOGGLE -> keyboardToggleCommandFactory.create()
        CommandIdentifier.TEXT_CURSOR_FORWARD -> textCursorForwardCommandFactory.create()
        CommandIdentifier.TEXT_CURSOR_BACKWARD -> textCursorBackwardCommandFactory.create()
        CommandIdentifier.TEXT_CURSOR_NEXT_WORD -> textCursorWordNextCommandFactory.create()
        CommandIdentifier.TEXT_CURSOR_PREVIOUS_WORD -> textCursorWordPreviousCommandFactory.create()
        CommandIdentifier.TEXT_DELETE_CHARACTER -> textDeleteCharCommandFactory.create()
        CommandIdentifier.TEXT_DELETE_WORD -> textDeleteWordCommandFactory.create()
        CommandIdentifier.TEXT_ENTER -> textEnterCommandFactory.create()
        CommandIdentifier.TEXT_CONFIRM -> textConfirmCommandFactory.create()
        CommandIdentifier.TEXT_CANCEL -> textCancelCommandFactory.create()
        CommandIdentifier.TEXT_EDIT -> textEditCommandFactory.create()
        CommandIdentifier.TEXT_PUNCTUATION_PERIOD -> textPunctuationCommandFactory.create(".")
        CommandIdentifier.TEXT_PUNCTUATION_COMMA -> textPunctuationCommandFactory.create(",")
        CommandIdentifier.TEXT_PUNCTUATION_QUESTION -> textPunctuationCommandFactory.create("?")
        CommandIdentifier.TEXT_PUNCTUATION_EXCLAMATION -> textPunctuationCommandFactory.create("!")

        // Phone calls
        CommandIdentifier.PHONE_PICK_UP -> pickUpCommandFactory.create()
        CommandIdentifier.PHONE_DECLINE -> declineCommandFactory.create()
        CommandIdentifier.PHONE_END_CALL -> endCommandFactory.create()
        CommandIdentifier.PHONE_SPEAKER -> speakerCommandFactory.create()
        CommandIdentifier.PHONE_CALL ->
            callCommandFactory.create(recipient = intent.textArgument())

        CommandIdentifier.SMS_WRITE ->
            writeSMSCommandFactory.create(recipient = intent.textArgument())

        // Overlay
        CommandIdentifier.OVERLAY_STOP -> sleepCommandFactory.create()

        // Media
        CommandIdentifier.VOLUME_UP -> volumeUpCommandFactory.create()
        CommandIdentifier.VOLUME_DOWN -> volumeDownCommandFactory.create()
        CommandIdentifier.MEDIA_PLAY -> playCommandFactory.create()
        CommandIdentifier.MEDIA_PAUSE -> pauseCommandFactory.create()
        CommandIdentifier.MEDIA_SKIP_FORWARD -> skipForwardCommandFactory.create(intent.intArgument())
        CommandIdentifier.MEDIA_SKIP_BACKWARD -> skipBackwardCommandFactory.create(intent.intArgument())
        CommandIdentifier.MEDIA_NEXT_TRACK -> nextTrackCommandFactory.create()
        CommandIdentifier.MEDIA_PREVIOUS_TRACK -> previousTrackCommandFactory.create()

        // Macro

        CommandIdentifier.MACRO_SHOW -> showMacroOverlayCommandFactory.create()
        CommandIdentifier.MACRO_HIDE -> hideMacroOverlayCommandFactory.create()
        CommandIdentifier.MACRO_TOGGLE -> toggleMacroOverlayCommandFactory.create()
        CommandIdentifier.MACRO_PLAY -> {
            val arg = intent.intArgument()
            if (arg != null)
                playMacroCommandFactory.create(arg - 1)
            else
                voidCommand(intent)
        }

        CommandIdentifier.MACRO_RECORD -> recordMacroCommandFactory.create(
            (intent.argument as? CommandArgument.Text)?.value == CommandIntentParser.RECORD_HERE
        )

        CommandIdentifier.MACRO_FINISH -> finishMacroCommandFactory.create()
        CommandIdentifier.MACRO_DELETE -> {
            val arg = intent.intArgument()
            if (arg != null)
                deleteMacroCommandFactory.create(arg - 1)
            else
                voidCommand(intent)
        }

        CommandIdentifier.MACRO_KEEP -> {
            val arg = intent.intArgument()
            if (arg != null)
                keepMacroCommandFactory.create(arg - 1)
            else
                voidCommand(intent)
        }

        CommandIdentifier.MACRO_RECORDING_DELETE_LAST -> deleteLastRecordedActionCommandFactory.create()

        // Prediction
        CommandIdentifier.PREDICTION_CONFIRM -> predictionConfirmCommandFactory.create()
        CommandIdentifier.PREDICTION_DISMISS -> predictionDismissCommandFactory.create()

        else -> LogCommand(intent.identifier, intent.argument)
    }


    /** Numbers refer to the labels while they are shown and the grid is not. */
    private fun labelsAddressed(): Boolean =
        labelsExecutorProvider.get()?.isShowing == true && gridExecutorProvider.get()?.isEnabled() != true

    private fun VoiceIntent.textArgument(): String? =
        (argument as? CommandArgument.Text)?.value

    private fun VoiceIntent.intArgument(): Int? =
        (argument as? CommandArgument.Number)?.value

    private fun VoiceIntent.swipeForce(): Int =
        intArgument()?.coerceIn(1, MAX_SWIPE_FORCE) ?: DEFAULT_SWIPE_FORCE

    // fallback when a blank command is needed (user did not provide an argument for example)
    private fun voidCommand(intent: VoiceIntent): Command {
        return LogCommand(intent.identifier, intent.argument)
    }
}
