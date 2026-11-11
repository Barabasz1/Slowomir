package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command
import javax.inject.Inject
import javax.inject.Singleton


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

    private fun save(command: Command) {

    }

    fun attachListener(listener: ICommandLoggerListener) {
        this.listener = listener
    }

    fun detachListener() {
        this.listener = null
    }

    private fun notifyListener(command: Command) {
        this.listener?.update(command)
    }
}