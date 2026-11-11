package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.log.CommandLogger
import javax.inject.Inject
import javax.inject.Singleton

// Experimental macro class structure, feel free to change everything
@Singleton
class MacroRecorder @Inject constructor(
    private val commandLogger: CommandLogger,
    private val macroRepository: MacroRepository
) : CommandLogger.ICommandLoggerListener {

    private var list: MutableList<Command> = mutableListOf()
    fun startRecording(): Boolean {
        reset()
        commandLogger.attachListener(this)
        return true
    }

    fun finishRecording(): Boolean {
        commandLogger.detachListener()
        return true
    }

    fun discardRecording(): Boolean {
        reset()
        return true
    }

    fun acceptRecording(): Boolean {
        save()
        reset()
        return true
    }

    override fun update(command: Command) {
        list.add(command)
    }




    fun isRecordingEmpty(): Boolean {
        return list.isEmpty()
    }





    private fun save() {
        macroRepository.saveRecordedMacro(list)
    }

    private fun reset() {
        list.clear()
    }
}
