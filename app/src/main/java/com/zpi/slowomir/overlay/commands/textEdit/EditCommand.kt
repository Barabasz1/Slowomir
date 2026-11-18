package com.zpi.slowomir.overlay.commands.textEdit

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that switches dictation to edit mode, where the cursor can be moved and text deleted.
 *
 * @param textEditExecutorProvider A provider for the [TextEditExecutor].
 */
class EditCommand @AssistedInject constructor(
    private val textEditExecutorProvider: Provider<TextEditExecutor?>,
) : Command {
    override val requiresBackgroundExecution: Boolean = true

    override fun execute(): CommandResult {
        val executor = textEditExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.edit()
        return if (success) CommandResult.Success() else CommandResult.Failure("Text edit mode failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): EditCommand
    }
}
