package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.grid.DivideCellCommand
import com.zpi.slowomir.overlay.commands.grid.GridEnableCommand
import com.zpi.slowomir.overlay.commands.grid.GridToggleCommand
import com.zpi.slowomir.overlay.commands.grid.SelectCellCommand
import javax.inject.Inject
import javax.inject.Singleton

// Experimental macro class structure, feel free to change everything
@Singleton
class MacroSupplier @Inject constructor(
    private val macroRepository: MacroRepository,
    private val gridEnableCommandFactory: GridEnableCommand.Factory,
    private val selectCellCommandFactory: SelectCellCommand.Factory,
    private val divideCellCommandFactory: DivideCellCommand.Factory,
    private val resetGridCommandFactory: DivideCellCommand.Factory
) {

    private val macroList: MutableList<MacroInfo> = mutableListOf()

    init {
        loadList()
    }

    fun getMacroFromList(index: Int): MacroInfo {
        return macroList[index]
    }

    fun getMacroList(): List<MacroInfo> {
        return macroList.toList()
    }

    private fun loadList() {
        loadRecordedMacros()
        loadGeneratedMacros()
    }

    private fun loadGeneratedMacros() {
        macroList.add(
            MacroInfo(
                commands = listOf(
                    gridEnableCommandFactory.create(),
                    selectCellCommandFactory.create(6),
                    divideCellCommandFactory.create(),
                    selectCellCommandFactory.create(4),
                    divideCellCommandFactory.create(),
                ),
                startingPoint = null,
                isGenerated = true
            )
        )
    }

    private fun loadRecordedMacros() {
        macroList.addAll(
            macroRepository.getRecordedMacros()
                .map {
                    MacroInfo(
                        it.commands,
                        it.startingPoint,
                        false
                    )
                }
        )
    }
}
