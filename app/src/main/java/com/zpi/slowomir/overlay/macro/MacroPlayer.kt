package com.zpi.slowomir.overlay.macro

import android.os.SystemClock
import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.commands.navigation.NavigateBackCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.NavigateRecentCommand
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.services.ScreenEventsBus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MacroPlayer @Inject constructor(
    private val mediator: CommandMediator,
    private val navigateHomeCommandFactory: NavigateHomeCommand.Factory,
    private val openAppCommandFactory: OpenAppCommand.Factory,
    private val playbackState: MacroPlaybackState,
    private val sectionDetector: ScreenSectionDetector,
    private val screenEventsBus: ScreenEventsBus,
) {
    companion object {
        private const val TAG = "MacroPlayer"
        private const val DEFAULT_COMMAND_INTERVAL: Long = 150L
        private const val APP_MIN_WAIT: Long = 1000L
        private const val APP_QUIET: Long = 700L
        private const val APP_MAX_WAIT: Long = 5000L
        private const val SECTION_MAX_WAIT: Long = 4000L
        private const val WAIT_STEP: Long = 25L
    }

    private val delayAfterCommand: Map<Class<out Command>, Long> = mapOf(
        NavigateHomeCommand::class.java to 400L,
        NavigateBackCommand::class.java to 300L,
        NavigateRecentCommand::class.java to 500L,
    )

    private fun delayAfter(command: Command): Long =
        delayAfterCommand[command::class.java] ?: DEFAULT_COMMAND_INTERVAL

    @Volatile
    private var stopMacro: Boolean = false

    fun play(macro: Macro): Boolean {
        val startedAt = SystemClock.elapsedRealtime()
        Log.i(TAG, "Play: ${macro.commands.size} commands, startingPoint=${macro.startingPoint}")
        stopMacro = false
        playbackState.start(macro.commands.size)

        try {
            if (!navigateToMacroStart(macro.startingPoint)) return aborted(
                startedAt,
                "stopped while going to the starting point"
            )

            for ((index, command) in macro.commands.withIndex()) {
                if (stopMacro) return aborted(startedAt, "stopped before command ${index + 1}")

                if (index > 0) {
                    val waited = waitFor(delayAfter(macro.commands[index - 1]))
                    Log.d(TAG, "[${index + 1}/${macro.commands.size}] waited ${waited}ms")
                    if (stopMacro) return aborted(
                        startedAt,
                        "stopped while waiting before command ${index + 1}"
                    )
                }

                val dispatchStart = SystemClock.elapsedRealtime()
                val packageBefore = foregroundPackage()
                val result = runBlocking { mediator.dispatch(command, logged = false) }
                if (command is OpenAppCommand && result is CommandResult.Success) awaitScreenStable(
                    packageBefore
                )
                Log.d(
                    TAG,
                    "[${index + 1}/${macro.commands.size}] ${command::class.java.simpleName} ${command.arguments} " +
                            "-> ${result.describe()} in ${SystemClock.elapsedRealtime() - dispatchStart}ms"
                )
                playbackState.update(macro.commands.size - index - 1)
            }

            Log.i(TAG, "Finished in ${SystemClock.elapsedRealtime() - startedAt}ms")
            return true
        } finally {
            stopMacro = false
            playbackState.finish()
        }
    }

    fun stop() {
        Log.i(TAG, "Stop requested")
        stopMacro = true
    }

    private fun aborted(startedAt: Long, reason: String): Boolean {
        Log.i(TAG, "Aborted after ${SystemClock.elapsedRealtime() - startedAt}ms: $reason")
        return false
    }

    private fun CommandResult.describe(): String = when (this) {
        is CommandResult.Success -> "success"
        is CommandResult.Failure -> "FAILED ($reason)"
        is CommandResult.InvalidContext -> "REJECTED ($reason)"
    }

    private fun foregroundPackage(): String? =
        CommandExecutorService.instance?.rootInActiveWindow?.packageName?.toString()

    private fun awaitScreenStable(packageBefore: String?): Boolean {
        val start = SystemClock.elapsedRealtime()
        val stable = runBlocking {
            withTimeoutOrNull(APP_MAX_WAIT.milliseconds) {
                coroutineScope {
                    var lastEvent = SystemClock.elapsedRealtime()
                    val listener = launch {
                        screenEventsBus.types.collect {
                            lastEvent = SystemClock.elapsedRealtime()
                        }
                    }
                    try {
                        while (!stopMacro) {
                            delay((WAIT_STEP * 2).milliseconds)
                            val now = SystemClock.elapsedRealtime()
                            val pkg = foregroundPackage()
                            val inForeground =
                                pkg != null && (pkg != packageBefore || now - start >= APP_MIN_WAIT)
                            if (inForeground && now - lastEvent >= APP_QUIET) break
                        }
                    } finally {
                        listener.cancel()
                    }
                    true
                }
            }
        } ?: false
        Log.d(TAG, "Screen stable=$stable after ${SystemClock.elapsedRealtime() - start}ms")
        return stable
    }

    private fun waitFor(millis: Long): Long {
        val start = SystemClock.elapsedRealtime()
        while (!stopMacro) {
            val left = millis - (SystemClock.elapsedRealtime() - start)
            if (left <= 0) break
            try {
                Thread.sleep(minOf(WAIT_STEP, left))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                stopMacro = true
            }
        }
        return SystemClock.elapsedRealtime() - start
    }

    private fun navigateToMacroStart(startingPoint: MacroStartingPoint?): Boolean {
        val command = when (startingPoint) {
            null -> return true
            is MacroStartingPoint.Home -> navigateHomeCommandFactory.create()
            is MacroStartingPoint.App -> openAppCommandFactory.create(startingPoint.appName)
        }
        val packageBefore = foregroundPackage()
        val result = runBlocking { mediator.dispatch(command, logged = false) }
        Log.d(TAG, "Starting point $startingPoint -> ${result.describe()}")

        if (startingPoint is MacroStartingPoint.App) {
            awaitScreenStable(packageBefore)
        } else {
            waitFor(DEFAULT_COMMAND_INTERVAL)
        }
        if (stopMacro) return false

        if (startingPoint is MacroStartingPoint.App && startingPoint.section != null) {
            openSection(startingPoint.section)
        }
        return !stopMacro
    }

    private fun openSection(section: String) {
        val start = SystemClock.elapsedRealtime()
        while (!stopMacro && SystemClock.elapsedRealtime() - start < SECTION_MAX_WAIT) {
            if (sectionDetector.currentSection().equals(section, ignoreCase = true)) {
                Log.d(TAG, "In section '$section'")
                return
            }
            val opened = sectionDetector.open(section)
            Log.d(TAG, "Opening section '$section' -> $opened")
            waitFor(if (opened) DEFAULT_COMMAND_INTERVAL else 300L)
        }
        Log.w(TAG, "Could not switch to section '$section'")
    }
}
