package com.zpi.slowomir.overlay.commands.textEdit

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that types dictated text into the focused text field.
 *
 * @param textEditExecutorProvider A provider for the [TextEditExecutor].
 * @param text The text to type.
 */
class DictateCommand @AssistedInject constructor(
    private val textEditExecutorProvider: Provider<TextEditExecutor?>,
    @Assisted("text") private val text: String,
) : Command {
    override val requiresBackgroundExecution: Boolean = true

    override fun execute(): CommandResult {
        val executor = textEditExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.insertText(text)
        return if (success) CommandResult.Success() else CommandResult.Failure("Text dictation failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("text") text: String): DictateCommand
    }
}
