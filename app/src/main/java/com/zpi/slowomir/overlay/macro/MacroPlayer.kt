package com.zpi.slowomir.overlay.macro

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.voice.mediator.CommandMediator
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

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

    @Volatile
    var isPlaying: Boolean = false
        private set

    fun play(macro: Macro): Boolean {
        isPlaying = true
        try {
            onMacroStart(macro)

            if (macro.startingPoint != null) {
                if (!wait(macro.startingPoint)) {
                    onMacroBreak(macro)
                    return false
                }
            }

            for ((index, command) in macro.commands.withIndex()) {

                if (stopMacro) {
                    onMacroBreak(macro)
                    return false
                }

                runBlocking {
                    mediator.dispatch(command)
                }

                val overrideDelay = macro.delaysMs.getOrNull(index)?.takeIf { it > 0L }
                if (!wait(command, overrideDelay)) {
                    onMacroBreak(macro)
                    return false
                }
            }

            onMacroFinish(macro)
            return true
        } finally {
            isPlaying = false
        }
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

    private fun calculateSleepTime(previousCommand: Command): Long {
        return when (previousCommand) {
            is OpenAppCommand -> OPEN_APP_DELAY
            else -> DEFAULT_COMMAND_INTERVAL
        }
    }

    private fun calculateSleepTime(startingPoint: MacroStartingPoint?): Long {
        return when (startingPoint) {
            is MacroStartingPoint.App -> OPEN_APP_DELAY
            else -> DEFAULT_COMMAND_INTERVAL
        }
    }

    private fun wait(previousCommand: Command, overrideDelay: Long? = null): Boolean {
        var elapsed = 0L
        val sleepTime: Long = overrideDelay ?: calculateSleepTime(previousCommand)
        while (elapsed < sleepTime) {
            if (stopMacro) return false
            val step = minOf(WAIT_STEP, sleepTime - elapsed)
            try {
                Thread.sleep(step)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
            elapsed += step
        }
        return true
    }

    private fun wait(startingPoint: MacroStartingPoint): Boolean {
        var elapsed = 0L
        val sleepTime: Long = calculateSleepTime(startingPoint)
        while (elapsed < sleepTime) {
            if (stopMacro) return false
            val step = minOf(WAIT_STEP, sleepTime - elapsed)
            try {
                Thread.sleep(step)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
            elapsed += step
        }
        return true
    }

    private fun navigateToMacroStart(macroStartingPoint: MacroStartingPoint?) {
        if (macroStartingPoint == null) return

        when (macroStartingPoint) {
            is MacroStartingPoint.Home ->
                runBlocking {
                    mediator.dispatch(navigateHomeCommandFactory.create())
                }

            is MacroStartingPoint.App ->
                runBlocking {
                    mediator.dispatch(openAppCommandFactory.create(macroStartingPoint.appName))
                }
        }
    }
}
