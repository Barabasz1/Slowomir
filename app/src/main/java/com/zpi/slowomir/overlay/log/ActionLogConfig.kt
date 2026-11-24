package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.LogCommand
import com.zpi.slowomir.overlay.commands.macro.AcceptRecordingMacroCommand
import com.zpi.slowomir.overlay.commands.macro.DiscardRecordingMacroCommand
import com.zpi.slowomir.overlay.commands.macro.FinishMacroCommand
import com.zpi.slowomir.overlay.commands.macro.HideMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.macro.PlayMacroCommand
import com.zpi.slowomir.overlay.commands.macro.RecordMacroCommand
import com.zpi.slowomir.overlay.commands.macro.ShowMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.macro.StopMacroCommand
import com.zpi.slowomir.overlay.commands.macro.ToggleMacroOverlayCommand
import com.zpi.slowomir.overlay.commands.predictions.ConfirmCommand
import com.zpi.slowomir.overlay.commands.predictions.DismissCommand

object ActionLogConfig {
    val isEnabled: Boolean = true
    const val RETENTION_DAYS = 90L
    const val PRUNE_INTERVAL_HOURS = 24L
    val clearOnStart: Boolean = false
    val logRowsToConsole: Boolean = true
    const val CONSOLE_TAG = "ActionLog"

    val excludedCommands: Set<Class<out Command>> = setOf(
        RecordMacroCommand::class.java,
        FinishMacroCommand::class.java,
        AcceptRecordingMacroCommand::class.java,
        DiscardRecordingMacroCommand::class.java,
        ShowMacroOverlayCommand::class.java,
        HideMacroOverlayCommand::class.java,
        ToggleMacroOverlayCommand::class.java,
        PlayMacroCommand::class.java,
        StopMacroCommand::class.java,
        ConfirmCommand::class.java,
        DismissCommand::class.java,
        LogCommand::class.java,
    )

    fun shouldLog(command: Command): Boolean {
        return isEnabled && command::class.java !in excludedCommands
    }
}