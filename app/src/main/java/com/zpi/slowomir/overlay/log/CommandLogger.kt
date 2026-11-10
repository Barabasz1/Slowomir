package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class CommandLogger @Inject constructor() : ICommandLogger {

    override fun log(command: Command) {

        // preprocess - filter, .. etc

        save(command)
    }


    private fun save(command: Command) {

    }
}