package com.zpi.slowomir.overlay.macro

import javax.inject.Inject
import javax.inject.Singleton

// Experimental macro class structure, feel free to change everything
@Singleton
class MacroSupplier @Inject constructor(
    private val macroRepository: MacroRepository
) {

    private val macroList: MutableList<Macro> = mutableListOf()

    init {
        loadList()
    }

    fun getMacroFromList(index: Int): Macro {
        return macroList[index]
    }

    private fun loadList() {
        loadRecordedMacros()
        loadGeneratedMacros()
    }

    private fun loadGeneratedMacros() {
    }

    private fun loadRecordedMacros() {
        macroList.addAll(macroRepository.getRecordedMacros())
    }
}
