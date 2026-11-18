package com.zpi.slowomir.overlay.macro

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroSupplier @Inject constructor(
    private val macroRepository: MacroRepository,
) {
    private val macroList: MutableList<MacroInfo> = mutableListOf()

    init {
        refresh()
    }

    @Synchronized
    fun getMacroFromList(index: Int): MacroInfo {
        if (macroList.isEmpty()) {
            refresh()
        }
        return macroList[index]
    }

    @Synchronized
    fun getMacroList(): List<MacroInfo> {
        refresh()
        return macroList.toList()
    }

    @Synchronized
    fun refresh(currentPackageName: String? = null) {
        macroList.clear()
        loadRecordedMacros()
        loadGeneratedMacros(currentPackageName)
    }

    private fun loadGeneratedMacros(currentPackageName: String?) {
        macroList.addAll(
            macroRepository.getGeneratedMacros(currentPackageName = currentPackageName)
                .map {
                    MacroInfo(
                        commands = it.commands,
                        startingPoint = it.startingPoint,
                        isGenerated = true,
                    )
                }
        )
    }

    private fun loadRecordedMacros() {
        macroList.addAll(
            macroRepository.getRecordedMacros()
                .map {
                    MacroInfo(
                        commands = it.commands,
                        startingPoint = it.startingPoint,
                        isGenerated = false,
                    )
                }
        )
    }
}