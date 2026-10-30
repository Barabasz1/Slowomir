package com.zpi.slowomir.overlay.commands.textEdit

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that moves the text editing cursor forward to the next word.
 *
 * @param textEditExecutorProvider A provider for the [TextEditExecutor].
 */
class CursorWordNextCommand @AssistedInject constructor(
    private val textEditExecutorProvider: Provider<TextEditExecutor?>,
) : Command {
    override fun execute(): CommandResult {
        val executor = textEditExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.cursorWordNext()
        return if (success) CommandResult.Success() else CommandResult.Failure("Text cursor next word failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(): CursorWordNextCommand
    }
}