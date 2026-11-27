package com.zpi.slowomir.overlay.commands.overlay

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.LabelsExecutor
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionLoggable
import com.zpi.slowomir.overlay.services.CommandExecutorService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * Taps (or long taps) a label of the labels overlay, chosen either by its number or by the text inside it.
 */
class SelectLabelCommand @AssistedInject constructor(
    private val labelsExecutorProvider: Provider<LabelsExecutor?>,
    @Assisted("number") private val number: Int?,
    @Assisted("text") private val text: String?,
    @Assisted("long") private val long: Boolean,
) : Command, ActionLoggable {

    private var target: ActionDescriptor? = null

    override val arguments: Map<String, Any?>
        get() = mapOf("number" to number, "text" to text, "long" to long)

    override fun consumeLoggedAction(): ActionDescriptor? = target

    override fun execute(): CommandResult {
        if (CommandExecutorService.instance == null) {
            return CommandResult.Failure("Accessibility service is not enabled")
        }
        val executor = labelsExecutorProvider.get()
            ?: return CommandResult.Failure("Labels executor is null")
        target = null
        if (!executor.select(number, text, long)) return CommandResult.Failure("No such label")
        target = executor.consumeLastTarget()
        return CommandResult.Success()
    }

    override fun canExecute(context: OverlayContext): Boolean =
        CommandExecutorService.instance != null && labelsExecutorProvider.get()?.isShowing == true

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("number") number: Int?,
            @Assisted("text") text: String?,
            @Assisted("long") long: Boolean,
        ): SelectLabelCommand
    }
}
