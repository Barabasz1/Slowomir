package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger for commands. It saves and logs commands.
 */
@Singleton
class CommandLogger @Inject constructor() : ICommandLogger {
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
     * Saves the command.
     *
     * @param command The command to save.
     */
    private fun save(command: Command) {

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