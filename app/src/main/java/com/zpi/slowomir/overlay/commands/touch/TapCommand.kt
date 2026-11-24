package com.zpi.slowomir.overlay.commands.touch

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TouchExecutor
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionLoggable
import com.zpi.slowomir.overlay.log.UiNodeDescriber
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A command that simulates a tap gesture at the specified screen coordinates.
 * A successful tap is stored in the action log as `UI_CLICK`.
 *
 * @param touchExecutorProvider A provider for the touch executor.
 * @param uiNodeDescriber Describes the UI element located under the tapped point.
 * @param x The x-coordinate of the tap location.
 * @param y The y-coordinate of the tap location.
 */
class TapCommand @AssistedInject constructor(
    private val touchExecutorProvider: Provider<TouchExecutor?>,
    private val uiNodeDescriber: UiNodeDescriber,
    @Assisted("x") private val x: Float,
    @Assisted("y") private val y: Float,
) : Command, ActionLoggable {

    override val arguments: Map<String, Any?>
        get() = mapOf("x" to x, "y" to y)

    private var loggedAction: ActionDescriptor? = null

    override fun execute(): CommandResult {
        loggedAction = null

        val executor = touchExecutorProvider.get()
            ?: return CommandResult.Failure("Accessibility Service is not enabled")

        val action = uiNodeDescriber.describeTapAt(x, y)

        val success = executor.shortTap(x, y)
        if (!success) return CommandResult.Failure("Tap failed")

        loggedAction = action
        return CommandResult.Success()
    }

    override fun consumeLoggedAction(): ActionDescriptor? {
        return loggedAction.also { loggedAction = null }
    }

    override fun canExecute(context: OverlayContext): Boolean {
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("x") x: Float,
            @Assisted("y") y: Float,
        ): TapCommand
    }
}
