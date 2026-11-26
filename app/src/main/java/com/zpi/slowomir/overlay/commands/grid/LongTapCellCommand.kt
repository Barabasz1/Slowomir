package com.zpi.slowomir.overlay.commands.grid

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.GridExecutor
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionLoggable
import com.zpi.slowomir.overlay.executors.TouchExecutor.Companion.DEFAULT_HOLD_DURATION
import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider


class LongTapCellCommand @AssistedInject constructor(
    private val gridExecutorProvider: Provider<GridExecutor?>,
    @Assisted("duration") private val duration: Long = DEFAULT_HOLD_DURATION,
) : Command, ActionLoggable {
    private var target: ActionDescriptor? = null

    override val arguments: Map<String, Any?>
        get() = mapOf("duration" to duration, "targetLabel" to target?.label)

    override fun consumeLoggedAction(): ActionDescriptor? = target


    override fun execute(): CommandResult {
        val gridExecutor = gridExecutorProvider.get()
            ?: return CommandResult.Failure("Grid executor is null")
        target = null
        if (gridExecutor.longTapCell(duration)) target = gridExecutor.consumeLastTarget()
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return CommandExecutorService.instance != null && (gridExecutorProvider.get()?.canExecuteActionOnCell() ?: false)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("duration") duration: Long = DEFAULT_HOLD_DURATION
        ): LongTapCellCommand
    }
}