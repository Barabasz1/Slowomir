package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command

interface ICommandLogger {
    fun log(command: Command)
}