package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command
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
        private const val DEFAULT_COMMAND_INTERVAL: Long = 500L
        private const val OPEN_APP_DELAY: Long = 2000L
        private const val WAIT_STEP: Long = 50L
    }

    @Volatile
    private var stopMacro: Boolean = false

    fun play(macro: Macro): Boolean {

        onMacroStart(macro)

        if (macro.startingPoint != null) {
            wait(macro.startingPoint)
        }

        for (command in macro.commands) {

            if (stopMacro) {
                onMacroBreak(macro)
                return false
            }

            mediator.dispatchScoped(command)

            wait(command)
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

    private fun calculateSleepTime(previousCommand: Command) : Long{
        return when(previousCommand) {
            // temporary fix: apps open more than DEFAULT_COMMAND_INTERVAL
            // check if app opened already
            is OpenAppCommand -> OPEN_APP_DELAY
            else -> DEFAULT_COMMAND_INTERVAL
        }
    }
    private fun calculateSleepTime(startingPoint: MacroStartingPoint?) : Long{
        return when(startingPoint) {
            // temporary fix: apps open more than DEFAULT_COMMAND_INTERVAL
            // check if app opened already
            is MacroStartingPoint.App -> OPEN_APP_DELAY
            else -> DEFAULT_COMMAND_INTERVAL
        }
    }
    private fun wait(previousCommand: Command): Boolean {
        var elapsed = 0L
        val sleepTime: Long = calculateSleepTime(previousCommand)
        while (elapsed < sleepTime) {
            val sleepTime = minOf(WAIT_STEP, sleepTime - elapsed)
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
    private fun wait(startingPoint: MacroStartingPoint): Boolean {
        var elapsed = 0L
        val sleepTime: Long = calculateSleepTime(startingPoint)
        while (elapsed < sleepTime) {
            val sleepTime = minOf(WAIT_STEP, sleepTime - elapsed)
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
