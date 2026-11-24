package com.zpi.slowomir.overlay.commands

import com.zpi.slowomir.overlay.commands.grid.AddColumnGridCommand
import com.zpi.slowomir.overlay.commands.grid.AddRowGridCommand
import com.zpi.slowomir.overlay.commands.grid.DivideCellCommand
import com.zpi.slowomir.overlay.commands.grid.GridDisableCommand
import com.zpi.slowomir.overlay.commands.grid.GridEnableCommand
import com.zpi.slowomir.overlay.commands.grid.GridResetCommand
import com.zpi.slowomir.overlay.commands.grid.GridToggleCommand
import com.zpi.slowomir.overlay.commands.grid.LongTapCellCommand
import com.zpi.slowomir.overlay.commands.grid.SelectCellCommand
import com.zpi.slowomir.overlay.commands.grid.SubtractColumnGridCommand
import com.zpi.slowomir.overlay.commands.grid.SubtractRowGridCommand
import com.zpi.slowomir.overlay.commands.grid.SwipeCellCommand
import com.zpi.slowomir.overlay.commands.grid.SwipeDownCellCommand
import com.zpi.slowomir.overlay.commands.grid.SwipeLeftCellCommand
import com.zpi.slowomir.overlay.commands.grid.SwipeRightCellCommand
import com.zpi.slowomir.overlay.commands.grid.SwipeUpCellCommand
import com.zpi.slowomir.overlay.commands.grid.TapCellCommand
import com.zpi.slowomir.overlay.commands.grid.UndivideCellCommand
import com.zpi.slowomir.overlay.commands.grid.ZoomInCellCommand
import com.zpi.slowomir.overlay.commands.grid.ZoomOutCellCommand
import com.zpi.slowomir.overlay.commands.macro.AcceptRecordingMacroCommand
import com.zpi.slowomir.overlay.commands.macro.DiscardRecordingMacroCommand
import com.zpi.slowomir.overlay.commands.macro.FinishMacroCommand
import com.zpi.slowomir.overlay.commands.macro.HideMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.macro.PlayMacroCommand
import com.zpi.slowomir.overlay.commands.macro.RecordMacroCommand
import com.zpi.slowomir.overlay.commands.macro.ShowMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.macro.StopMacroCommand
import com.zpi.slowomir.overlay.commands.macro.ToggleMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.media.NextTrackCommand
import com.zpi.slowomir.overlay.commands.media.PauseCommand
import com.zpi.slowomir.overlay.commands.media.PreviousTrackCommand
import com.zpi.slowomir.overlay.commands.media.SkipBackwardCommand
import com.zpi.slowomir.overlay.commands.media.SkipForwardCommand
import com.zpi.slowomir.overlay.commands.media.PlayCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.LockScreenCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateBackCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateRecentCommand
import com.zpi.slowomir.overlay.commands.navigation.NotificationsCommand
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.overlay.commands.navigation.QuickSettingsCommand
import com.zpi.slowomir.overlay.commands.navigation.ScreenshotCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeDownNavCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeLeftNavCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeRightNavCommand
import com.zpi.slowomir.overlay.commands.navigation.SwipeUpNavCommand
import com.zpi.slowomir.overlay.commands.navigation.ZoomInNavCommand
import com.zpi.slowomir.overlay.commands.navigation.ZoomOutNavCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.CallCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.DeclineCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.EndCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.PickUpCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.SpeakerCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.WriteSMSCommand
import com.zpi.slowomir.overlay.commands.predictions.ConfirmCommand as PredictionConfirmCommand
import com.zpi.slowomir.overlay.commands.predictions.DismissCommand
import com.zpi.slowomir.overlay.commands.textEdit.ConfirmCommand as TextConfirmCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorBackwardCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorForwardCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorWordNextCommand
import com.zpi.slowomir.overlay.commands.textEdit.CursorWordPreviousCommand
import com.zpi.slowomir.overlay.commands.textEdit.DeleteCharCommand
import com.zpi.slowomir.overlay.commands.textEdit.DeleteWordCommand
import com.zpi.slowomir.overlay.commands.textEdit.EnterCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardToggleCommand
import com.zpi.slowomir.overlay.commands.touch.LongTapCommand
import com.zpi.slowomir.overlay.commands.touch.PinchInCommand
import com.zpi.slowomir.overlay.commands.touch.PinchOutCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeDownCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeLeftCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeRightCommand
import com.zpi.slowomir.overlay.commands.touch.SwipeUpCommand
import com.zpi.slowomir.overlay.commands.touch.TapCommand
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_HOLD_DURATION
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_PINCH_POWER
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_SWIPE_DISTANCE
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SerializedCommand(
    val type: String,
    val name: String,
    val arguments: Map<String, Any?>,
    val displayText: String,
) {
    val argumentsJson: String
        get() = CommandArgumentCodec.encode(arguments)
}

object CommandArgumentCodec {
    fun encode(arguments: Map<String, Any?>): String {
        val obj = JSONObject()
        for ((key, value) in arguments) {
            obj.put(key, value ?: JSONObject.NULL)
        }
        return obj.toString()
    }

    fun decode(json: String?): Map<String, Any?> {
        if (json.isNullOrBlank()) return emptyMap()
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        val result = linkedMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            result[key] = if (value === JSONObject.NULL) null else value
        }
        return result
    }
}

@Singleton
class CommandFactory @Inject constructor(
    private val addColumnGridCommandFactory: AddColumnGridCommand.Factory,
    private val addRowGridCommandFactory: AddRowGridCommand.Factory,
    private val divideCellCommandFactory: DivideCellCommand.Factory,
    private val gridDisableCommandFactory: GridDisableCommand.Factory,
    private val gridEnableCommandFactory: GridEnableCommand.Factory,
    private val gridResetCommandFactory: GridResetCommand.Factory,
    private val gridToggleCommandFactory: GridToggleCommand.Factory,
    private val longTapCellCommandFactory: LongTapCellCommand.Factory,
    private val selectCellCommandFactory: SelectCellCommand.Factory,
    private val subtractColumnGridCommandFactory: SubtractColumnGridCommand.Factory,
    private val subtractRowGridCommandFactory: SubtractRowGridCommand.Factory,
    private val swipeCellCommandFactory: SwipeCellCommand.Factory,
    private val swipeDownCellCommandFactory: SwipeDownCellCommand.Factory,
    private val swipeLeftCellCommandFactory: SwipeLeftCellCommand.Factory,
    private val swipeRightCellCommandFactory: SwipeRightCellCommand.Factory,
    private val swipeUpCellCommandFactory: SwipeUpCellCommand.Factory,
    private val tapCellCommandFactory: TapCellCommand.Factory,
    private val undivideCellCommandFactory: UndivideCellCommand.Factory,
    private val zoomInCellCommandFactory: ZoomInCellCommand.Factory,
    private val zoomOutCellCommandFactory: ZoomOutCellCommand.Factory,
    private val acceptRecordingMacroCommandFactory: AcceptRecordingMacroCommand.Factory,
    private val discardRecordingMacroCommandFactory: DiscardRecordingMacroCommand.Factory,
    private val finishMacroCommandFactory: FinishMacroCommand.Factory,
    private val hideMacroOverlayCommandFactory: HideMacroOverlayCommand.Factory,
    private val playMacroCommandFactory: PlayMacroCommand.Factory,
    private val recordMacroCommandFactory: RecordMacroCommand.Factory,
    private val showMacroOverlayCommandFactory: ShowMacroOverlayCommand.Factory,
    private val stopMacroCommandFactory: StopMacroCommand.Factory,
    private val toggleMacroOverlayCommandFactory: ToggleMacroOverlayCommand.Factory,
    private val pauseCommandFactory: PauseCommand.Factory,
    private val playCommandFactory: PlayCommand.Factory,
    private val volumeDownCommandFactory: VolumeDownCommand.Factory,
    private val skipForwardCommandFactory: SkipForwardCommand.Factory,
    private val skipBackwardCommandFactory: SkipBackwardCommand.Factory,
    private val nextTrackCommandFactory: NextTrackCommand.Factory,
    private val previousTrackCommandFactory: PreviousTrackCommand.Factory,
    private val volumeUpCommandFactory: VolumeUpCommand.Factory,
    private val lockScreenCommandFactory: LockScreenCommand.Factory,
    private val navigateBackCommandFactory: NavigateBackCommand.Factory,
    private val navigateHomeCommandFactory: NavigateHomeCommand.Factory,
    private val navigateRecentCommandFactory: NavigateRecentCommand.Factory,
    private val notificationsCommandFactory: NotificationsCommand.Factory,
    private val openAppCommandFactory: OpenAppCommand.Factory,
    private val quickSettingsCommandFactory: QuickSettingsCommand.Factory,
    private val screenshotCommandFactory: ScreenshotCommand.Factory,
    private val swipeDownNavCommandFactory: SwipeDownNavCommand.Factory,
    private val swipeLeftNavCommandFactory: SwipeLeftNavCommand.Factory,
    private val swipeRightNavCommandFactory: SwipeRightNavCommand.Factory,
    private val swipeUpNavCommandFactory: SwipeUpNavCommand.Factory,
    private val zoomInNavCommandFactory: ZoomInNavCommand.Factory,
    private val zoomOutNavCommandFactory: ZoomOutNavCommand.Factory,
    private val callCommandFactory: CallCommand.Factory,
    private val declineCommandFactory: DeclineCommand.Factory,
    private val endCommandFactory: EndCommand.Factory,
    private val pickUpCommandFactory: PickUpCommand.Factory,
    private val speakerCommandFactory: SpeakerCommand.Factory,
    private val writeSMSCommandFactory: WriteSMSCommand.Factory,
    private val predictionConfirmCommandFactory: PredictionConfirmCommand.Factory,
    private val dismissCommandFactory: DismissCommand.Factory,
    private val textConfirmCommandFactory: TextConfirmCommand.Factory,
    private val cursorBackwardCommandFactory: CursorBackwardCommand.Factory,
    private val cursorForwardCommandFactory: CursorForwardCommand.Factory,
    private val cursorWordNextCommandFactory: CursorWordNextCommand.Factory,
    private val cursorWordPreviousCommandFactory: CursorWordPreviousCommand.Factory,
    private val deleteCharCommandFactory: DeleteCharCommand.Factory,
    private val deleteWordCommandFactory: DeleteWordCommand.Factory,
    private val enterCommandFactory: EnterCommand.Factory,
    private val keyboardHideCommandFactory: KeyboardHideCommand.Factory,
    private val keyboardShowCommandFactory: KeyboardShowCommand.Factory,
    private val keyboardToggleCommandFactory: KeyboardToggleCommand.Factory,
    private val longTapCommandFactory: LongTapCommand.Factory,
    private val pinchInCommandFactory: PinchInCommand.Factory,
    private val pinchOutCommandFactory: PinchOutCommand.Factory,
    private val swipeCommandFactory: SwipeCommand.Factory,
    private val swipeDownCommandFactory: SwipeDownCommand.Factory,
    private val swipeLeftCommandFactory: SwipeLeftCommand.Factory,
    private val swipeRightCommandFactory: SwipeRightCommand.Factory,
    private val swipeUpCommandFactory: SwipeUpCommand.Factory,
    private val tapCommandFactory: TapCommand.Factory,
) {
    fun serialize(command: Command): SerializedCommand {
        return SerializedCommand(
            type = command.commandType,
            name = command::class.java.simpleName,
            arguments = command.arguments,
            displayText = command.formatDisplay(),
        )
    }

    fun create(serialized: SerializedCommand): Command? {
        return create(serialized.type, serialized.arguments)
    }

    fun createFromJson(type: String?, argumentsJson: String?): Command? {
        if (type.isNullOrBlank()) return null
        return create(type, CommandArgumentCodec.decode(argumentsJson))
    }

    fun create(type: String, arguments: Map<String, Any?> = emptyMap()): Command? {
        return when (type) {
            AddColumnGridCommand::class.java.name ->
                addColumnGridCommandFactory.create(arguments.intOrDefault("num", 1))

            AddRowGridCommand::class.java.name ->
                addRowGridCommandFactory.create(arguments.intOrDefault("num", 1))

            DivideCellCommand::class.java.name ->
                divideCellCommandFactory.create(arguments.intOrDefault("divisionPower", 2))

            GridDisableCommand::class.java.name ->
                gridDisableCommandFactory.create()

            GridEnableCommand::class.java.name ->
                gridEnableCommandFactory.create()

            GridResetCommand::class.java.name ->
                gridResetCommandFactory.create()

            GridToggleCommand::class.java.name ->
                gridToggleCommandFactory.create()

            LongTapCellCommand::class.java.name ->
                longTapCellCommandFactory.create(arguments.longOrDefault("duration", DEFAULT_HOLD_DURATION))

            SelectCellCommand::class.java.name ->
                selectCellCommandFactory.create(arguments.intOrDefault("cell", 1))

            SubtractColumnGridCommand::class.java.name ->
                subtractColumnGridCommandFactory.create(arguments.intOrDefault("num", 1))

            SubtractRowGridCommand::class.java.name ->
                subtractRowGridCommandFactory.create(arguments.intOrDefault("num", 1))

            SwipeCellCommand::class.java.name ->
                swipeCellCommandFactory.create(
                    arguments.intOrDefault("cell0", 1),
                    arguments.intOrDefault("cell1", 2),
                )

            SwipeDownCellCommand::class.java.name ->
                swipeDownCellCommandFactory.create(arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE))

            SwipeLeftCellCommand::class.java.name ->
                swipeLeftCellCommandFactory.create(arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE))

            SwipeRightCellCommand::class.java.name ->
                swipeRightCellCommandFactory.create(arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE))

            SwipeUpCellCommand::class.java.name ->
                swipeUpCellCommandFactory.create(arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE))

            TapCellCommand::class.java.name ->
                tapCellCommandFactory.create()

            UndivideCellCommand::class.java.name ->
                undivideCellCommandFactory.create()

            ZoomInCellCommand::class.java.name ->
                zoomInCellCommandFactory.create(arguments.floatOrDefault("power", DEFAULT_PINCH_POWER))

            ZoomOutCellCommand::class.java.name ->
                zoomOutCellCommandFactory.create(arguments.floatOrDefault("power", DEFAULT_PINCH_POWER))

            AcceptRecordingMacroCommand::class.java.name ->
                acceptRecordingMacroCommandFactory.create()

            DiscardRecordingMacroCommand::class.java.name ->
                discardRecordingMacroCommandFactory.create()

            FinishMacroCommand::class.java.name ->
                finishMacroCommandFactory.create()

            HideMacroOverlayCommand::class.java.name ->
                hideMacroOverlayCommandFactory.create()

            PlayMacroCommand::class.java.name ->
                playMacroCommandFactory.create(arguments.intOrDefault("index", 0))

            RecordMacroCommand::class.java.name ->
                recordMacroCommandFactory.create()

            ShowMacroOverlayCommand::class.java.name ->
                showMacroOverlayCommandFactory.create()

            StopMacroCommand::class.java.name ->
                stopMacroCommandFactory.create()

            ToggleMacroOverlayCommand::class.java.name ->
                toggleMacroOverlayCommandFactory.create()

            PauseCommand::class.java.name ->
                pauseCommandFactory.create()

            PlayCommand::class.java.name ->
                playCommandFactory.create()

            VolumeDownCommand::class.java.name ->
                volumeDownCommandFactory.create()

            VolumeUpCommand::class.java.name ->
                volumeUpCommandFactory.create()

            SkipForwardCommand::class.java.name ->
                skipForwardCommandFactory.create(arguments.intOrNull("seconds"))

            SkipBackwardCommand::class.java.name ->
                skipBackwardCommandFactory.create(arguments.intOrNull("seconds"))

            NextTrackCommand::class.java.name ->
                nextTrackCommandFactory.create()

            PreviousTrackCommand::class.java.name ->
                previousTrackCommandFactory.create()

            LockScreenCommand::class.java.name ->
                lockScreenCommandFactory.create()

            NavigateBackCommand::class.java.name ->
                navigateBackCommandFactory.create()

            NavigateHomeCommand::class.java.name ->
                navigateHomeCommandFactory.create()

            NavigateRecentCommand::class.java.name ->
                navigateRecentCommandFactory.create()

            NotificationsCommand::class.java.name ->
                notificationsCommandFactory.create()

            OpenAppCommand::class.java.name ->
                openAppCommandFactory.create(arguments.stringOrNull("packageName"))

            QuickSettingsCommand::class.java.name ->
                quickSettingsCommandFactory.create()

            ScreenshotCommand::class.java.name ->
                screenshotCommandFactory.create()

            SwipeDownNavCommand::class.java.name ->
                swipeDownNavCommandFactory.create()

            SwipeLeftNavCommand::class.java.name ->
                swipeLeftNavCommandFactory.create()

            SwipeRightNavCommand::class.java.name ->
                swipeRightNavCommandFactory.create()

            SwipeUpNavCommand::class.java.name ->
                swipeUpNavCommandFactory.create()

            ZoomInNavCommand::class.java.name ->
                zoomInNavCommandFactory.create()

            ZoomOutNavCommand::class.java.name ->
                zoomOutNavCommandFactory.create()

            CallCommand::class.java.name ->
                callCommandFactory.create(arguments.stringOrNull("recipient"))

            DeclineCommand::class.java.name ->
                declineCommandFactory.create()

            EndCommand::class.java.name ->
                endCommandFactory.create()

            PickUpCommand::class.java.name ->
                pickUpCommandFactory.create()

            SpeakerCommand::class.java.name ->
                speakerCommandFactory.create()

            WriteSMSCommand::class.java.name ->
                writeSMSCommandFactory.create(arguments.stringOrNull("recipient"))

            PredictionConfirmCommand::class.java.name ->
                predictionConfirmCommandFactory.create()

            DismissCommand::class.java.name ->
                dismissCommandFactory.create()

            TextConfirmCommand::class.java.name ->
                textConfirmCommandFactory.create()

            CursorBackwardCommand::class.java.name ->
                cursorBackwardCommandFactory.create()

            CursorForwardCommand::class.java.name ->
                cursorForwardCommandFactory.create()

            CursorWordNextCommand::class.java.name ->
                cursorWordNextCommandFactory.create()

            CursorWordPreviousCommand::class.java.name ->
                cursorWordPreviousCommandFactory.create()

            DeleteCharCommand::class.java.name ->
                deleteCharCommandFactory.create()

            DeleteWordCommand::class.java.name ->
                deleteWordCommandFactory.create()

            EnterCommand::class.java.name ->
                enterCommandFactory.create()

            KeyboardHideCommand::class.java.name ->
                keyboardHideCommandFactory.create()

            KeyboardShowCommand::class.java.name ->
                keyboardShowCommandFactory.create()

            KeyboardToggleCommand::class.java.name ->
                keyboardToggleCommandFactory.create()

            LongTapCommand::class.java.name ->
                longTapCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.longOrDefault("duration", DEFAULT_HOLD_DURATION),
                )

            PinchInCommand::class.java.name ->
                pinchInCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.floatOrDefault("power", DEFAULT_PINCH_POWER),
                )

            PinchOutCommand::class.java.name ->
                pinchOutCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.floatOrDefault("power", DEFAULT_PINCH_POWER),
                )

            SwipeCommand::class.java.name ->
                swipeCommandFactory.create(
                    arguments.floatOrDefault("startX", 0f),
                    arguments.floatOrDefault("startY", 0f),
                    arguments.floatOrDefault("destX", 0f),
                    arguments.floatOrDefault("destY", 0f),
                )

            SwipeDownCommand::class.java.name ->
                swipeDownCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE),
                )

            SwipeLeftCommand::class.java.name ->
                swipeLeftCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE),
                )

            SwipeRightCommand::class.java.name ->
                swipeRightCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE),
                )

            SwipeUpCommand::class.java.name ->
                swipeUpCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                    arguments.floatOrDefault("distance", DEFAULT_SWIPE_DISTANCE),
                )

            TapCommand::class.java.name ->
                tapCommandFactory.create(
                    arguments.floatOrDefault("x", 0f),
                    arguments.floatOrDefault("y", 0f),
                )

            LogCommand::class.java.name -> {
                val idName = arguments.stringOrNull("identifier") ?: return null
                val identifier = runCatching { CommandIdentifier.valueOf(idName) }.getOrNull() ?: return null
                val rawArg = arguments.stringOrNull("argument")
                val arg = rawArg?.let { CommandArgument.Text(it) }
                LogCommand(identifier, arg)
            }

            else -> null
        }
    }

    fun createOpenApp(packageName: String): Command = openAppCommandFactory.create(packageName)

    fun createTap(x: Float, y: Float): Command = tapCommandFactory.create(x, y)

    fun createNavigateHome(): Command = navigateHomeCommandFactory.create()

    fun createNavigateBack(): Command = navigateBackCommandFactory.create()

    fun createNavigateRecent(): Command = navigateRecentCommandFactory.create()

    private fun Map<String, Any?>.stringOrNull(key: String): String? {
        return this[key]?.toString()
    }

    private fun Map<String, Any?>.intOrDefault(key: String, default: Int): Int {
        return when (val v = this[key]) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }
    }

    private fun Map<String, Any?>.intOrNull(key: String): Int? {
        return when (val v = this[key]) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }

    private fun Map<String, Any?>.longOrDefault(key: String, default: Long): Long {
        return when (val v = this[key]) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun Map<String, Any?>.floatOrDefault(key: String, default: Float): Float {
        return when (val v = this[key]) {
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull() ?: default
            else -> default
        }
    }
}