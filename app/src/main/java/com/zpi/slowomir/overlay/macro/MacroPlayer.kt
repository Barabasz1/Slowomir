package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.voice.mediator.CommandMediator
import javax.inject.Inject
import javax.inject.Singleton

// Experimental macro class structure, feel free to change everything
@Singleton
class MacroPlayer @Inject constructor(
    private val mediator: CommandMediator,
    private val navigateHomeCommandFactory: NavigateHomeCommand.Factory,
    private val openAppCommandFactory: OpenAppCommand.Factory
) {
    companion object {
        private const val COMMAND_INTERVAL: Long = 500L
        private const val WAIT_STEP: Long = 50L
    }

    @Volatile
    private var stopMacro: Boolean = false

    fun play(macro: Macro): Boolean {

        onMacroStart(macro)

        if (macro.startingPoint != null) {
            wait(COMMAND_INTERVAL)
        }

        for (command in macro.commands) {

            if (stopMacro) {
                onMacroBreak(macro)
                return false
            }

            command.execute()

            wait(COMMAND_INTERVAL)
        }

        onMacroFinish(macro)
        return true
    }

    fun stop() {
        stopMacro = true
    }

    private fun onMacroStart(macro: Macro) {
        stopMacro = false
        navigateToMacroStart(macro.startingPoint)
    }

    private fun onMacroFinish(macro: Macro) {
        stopMacro = false
    }

    private fun onMacroBreak(macro: Macro) {
    }

    private fun wait(time: Long): Boolean {
        var elapsed = 0L
        while (elapsed < time) {
            val sleepTime = minOf(WAIT_STEP, time - elapsed)
            try {
                Thread.sleep(sleepTime)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
            elapsed += sleepTime
        }
        return true
    }

    private fun navigateToMacroStart(macroStartingPoint: MacroStartingPoint?) {
        if (macroStartingPoint == null) return

        when (macroStartingPoint) {
            is MacroStartingPoint.Home ->
                mediator.dispatchScoped(navigateHomeCommandFactory.create())

            is MacroStartingPoint.App ->
                mediator.dispatchScoped(openAppCommandFactory.create(macroStartingPoint.appName))
        }
    }
}
