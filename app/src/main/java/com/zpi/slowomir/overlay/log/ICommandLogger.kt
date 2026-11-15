package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command

/**
 * Logger interface for commands. It saves and logs commands.
 */
interface ICommandLogger {
    fun log(command: Command)
}