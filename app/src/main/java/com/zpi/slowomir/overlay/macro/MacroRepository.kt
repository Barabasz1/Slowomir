package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command
import javax.inject.Inject
import javax.inject.Singleton

// Experimental macro class structure, feel free to change everything
@Singleton
class MacroRepository @Inject constructor() {

    fun saveRecordedMacro(commandSequence: List<Command>) {

    }

    fun getRecordedMacros(limit: Int = 10): List<Macro> {
        return listOf()
    }
}
