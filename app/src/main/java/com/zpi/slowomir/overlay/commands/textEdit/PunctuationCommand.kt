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
 * A command that inserts a single punctuation mark at the text editing cursor.
 *
 * @param textEditExecutorProvider A provider for the [TextEditExecutor].
 * @param mark The punctuation mark to insert.
 */
class PunctuationCommand @AssistedInject constructor(
    private val textEditExecutorProvider: Provider<TextEditExecutor?>,
    @Assisted("mark") private val mark: String,
) : Command {
    override val requiresBackgroundExecution: Boolean = true

    override val arguments: Map<String, Any?>
        get() = mapOf("mark" to mark)

    override fun execute(): CommandResult {
        val executor = textEditExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val success = executor.insertPunctuation(mark)
        return if (success) CommandResult.Success() else CommandResult.Failure("Punctuation insert failed")
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("mark") mark: String): PunctuationCommand
    }
}
