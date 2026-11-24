package com.zpi.slowomir.overlay.macro

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroSupplier @Inject constructor(
    private val macroRepository: MacroRepository,
) {
    private val macroList: MutableList<Macro> = mutableListOf()

    init {
        refresh()
    }

    @Synchronized
    fun getMacroFromList(index: Int): Macro {
        if (macroList.isEmpty()) {
            refresh()
        }
        return macroList[index]
    }

    @Synchronized
    fun getMacroList(): List<Macro> {
        refresh()
        return macroList.toList()
    }

    @Synchronized
    fun refresh(currentPackageName: String? = null) {
        macroList.clear()
        loadRecordedMacros()
        loadGeneratedMacros(currentPackageName)

        while (macroList.size > MacroRepository.MAX_LISTED_MACROS) macroList.removeAt(macroList.lastIndex)
    }

    private fun loadGeneratedMacros(currentPackageName: String?) {
        macroList.addAll(
            macroRepository.getGeneratedMacros(currentPackageName = currentPackageName)
                .map { it.copy(isGenerated = true) }
        )
    }

    private fun loadRecordedMacros() {
        macroList.addAll(
            macroRepository.getRecordedMacros(limit = MacroRepository.MAX_LISTED_MACROS)
                .map { it.copy(isGenerated = false) }
        )
    }
}
