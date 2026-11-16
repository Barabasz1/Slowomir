package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger for commands. It saves and logs commands.
 *
 * Commands that implement [ActionLoggable] are additionally stored in the action log
 * (tables `action_def` / `action_event`) through [ActionRecorder].
 */
@Singleton
class CommandLogger @Inject constructor(
    private val actionRecorder: ActionRecorder,
) : ICommandLogger {
    interface ICommandLoggerListener {
        fun update(command: Command)
    }

    private var listener: ICommandLoggerListener? = null

    override fun log(command: Command) {

        // preprocess - filter, .. etc

        notifyListener(command)
        save(command)
    }

    /**
     * Saves the command in the action log.
     * Only successfully executed [ActionLoggable] commands produce an entry.
     *
     * @param command The command to save.
     */
    private fun save(command: Command) {
        val action = (command as? ActionLoggable)?.consumeLoggedAction() ?: return
        actionRecorder.record(action, System.currentTimeMillis())
    }

    /**
     * Attaches a listener to the logger.
     *
     * @param listener The listener to attach.
     */
    fun attachListener(listener: ICommandLoggerListener) {
        this.listener = listener
    }

    /**
     * Detaches the listener from the logger.
     */
    fun detachListener() {
        this.listener = null
    }

    /**
     * Notifies the listener about a new command.
     *
     * @param command The new command.
     */
    private fun notifyListener(command: Command) {
        this.listener?.update(command)
    }
}
