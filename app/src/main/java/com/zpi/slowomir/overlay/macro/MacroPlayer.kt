package com.zpi.slowomir.overlay.macro

import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.overlay.services.CommandExecutorService
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
        private const val MIN_WAIT_DEFAULT_MS: Long = 200L
        private const val MIN_WAIT_APP_MS: Long = 400L
        private const val QUIET_PERIOD_DEFAULT_MS: Long = 250L
        private const val QUIET_PERIOD_APP_MS: Long = 350L
        private const val MAX_TIMEOUT_DEFAULT_MS: Long = 3000L
        private const val MAX_TIMEOUT_APP_MS: Long = 5000L
        private const val CHECK_STEP_MS: Long = 50L
    }

    @Volatile
    private var stopMacro: Boolean = false

    fun play(macro: Macro): Boolean {
        onMacroStart(macro)

        if (macro.startingPoint != null) {
            navigateToMacroStart(macro.startingPoint)
            if (!waitForScreenStability(macro.startingPoint is MacroStartingPoint.App)) {
                onMacroBreak(macro)
                return false
            }
        }

        for (command in macro.commands) {
            if (stopMacro) {
                onMacroBreak(macro)
                return false
            }

            runBlocking {
                mediator.dispatch(command)
            }

            if (!waitForScreenStability(command is OpenAppCommand)) {
                onMacroBreak(macro)
                return false
            }
        }

        onMacroFinish(macro)
        return true
    }

    fun stop() {
        stopMacro = true
    }

    private fun onMacroStart(macro: Macro) {
        stopMacro = false
    }

    private fun onMacroFinish(macro: Macro) {
        stopMacro = false
    }

    private fun onMacroBreak(macro: Macro) {
        stopMacro = false
    }

    fun waitForScreenStability(isAppLaunchOrStart: Boolean = false): Boolean {
        val minWaitMs = if (isAppLaunchOrStart) MIN_WAIT_APP_MS else MIN_WAIT_DEFAULT_MS
        val quietPeriodMs = if (isAppLaunchOrStart) QUIET_PERIOD_APP_MS else QUIET_PERIOD_DEFAULT_MS
        val maxTimeoutMs = if (isAppLaunchOrStart) MAX_TIMEOUT_APP_MS else MAX_TIMEOUT_DEFAULT_MS

        val startTime = System.currentTimeMillis()
        var lastTreeSignature: Int? = null
        var stableTreeCount = 0

        while (true) {
            if (stopMacro) return false

            val now = System.currentTimeMillis()
            val elapsed = now - startTime

            if (elapsed >= maxTimeoutMs) {
                return true
            }

            if (elapsed >= minWaitMs) {
                val service = CommandExecutorService.instance
                val lastEventTime = service?.lastUiEventTimestamp ?: 0L
                val quietTime = if (lastEventTime > 0L) now - lastEventTime else elapsed

                val currentTreeSignature = getUiTreeSignature()

                if (quietTime >= quietPeriodMs) {
                    if (currentTreeSignature != null && currentTreeSignature == lastTreeSignature) {
                        stableTreeCount++
                        if (stableTreeCount >= 2) {
                            return true
                        }
                    } else {
                        stableTreeCount = 1
                        lastTreeSignature = currentTreeSignature
                        if (service == null || lastEventTime == 0L) {
                            if (quietTime >= quietPeriodMs) return true
                        }
                    }
                } else {
                    stableTreeCount = 0
                    lastTreeSignature = currentTreeSignature
                }
            }

            try {
                Thread.sleep(CHECK_STEP_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
    }

    private fun getUiTreeSignature(): Int? {
        val root = CommandExecutorService.instance?.rootInActiveWindow ?: return null
        var hash = 17
        var nodeCount = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || nodeCount > 100) return
            nodeCount++
            hash = 31 * hash + (node.className?.hashCode() ?: 0)
            hash = 31 * hash + (node.packageName?.hashCode() ?: 0)
            hash = 31 * hash + node.childCount
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i) ?: continue
                traverse(child)
            }
        }

        traverse(root)
        return 31 * hash + nodeCount
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
