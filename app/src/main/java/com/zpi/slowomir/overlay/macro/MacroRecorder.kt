package com.zpi.slowomir.overlay.macro

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.log.CommandLogger
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.voice.mediator.CommandMediator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val actionCount: Int) : RecordingState
    data class AwaitingConfirmation(
        val commands: List<Command>,
        val startingPoint: MacroStartingPoint?,
    ) : RecordingState
}

@Singleton
class MacroRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandLogger: CommandLogger,
    private val macroRepository: MacroRepository,
    private val macroSupplier: MacroSupplier,
    private val mediatorProvider: Provider<CommandMediator>,
    private val sectionDetector: ScreenSectionDetector,
) : CommandLogger.ICommandLoggerListener {

    private val list: MutableList<Command> = mutableListOf()
    private var recordedStartingPoint: MacroStartingPoint? = null

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun startRecording(rememberStartingPoint: Boolean = false): Boolean {
        reset()
        _state.value = RecordingState.Recording(0)
        recordedStartingPoint = if (rememberStartingPoint) detectCurrentStartingPoint() else null
        Log.d(
            TAG,
            "Start recording: here=$rememberStartingPoint -> startingPoint=$recordedStartingPoint"
        )
        commandLogger.attachListener(this)
        return true
    }

    fun finishRecording(): Boolean {
        if (_state.value !is RecordingState.Recording) return false
        commandLogger.detachListener()
        val keep = list.isNotEmpty() || recordedStartingPoint is MacroStartingPoint.App
        Log.d(
            TAG,
            "Finish recording: ${list.size} actions, startingPoint=$recordedStartingPoint, keep=$keep"
        )
        _state.value = if (!keep) {
            reset()
            RecordingState.Idle
        } else {
            RecordingState.AwaitingConfirmation(list.toList(), recordedStartingPoint)
        }
        return true
    }

    fun discardRecording(): Boolean {
        commandLogger.detachListener()
        reset()
        _state.value = RecordingState.Idle
        return true
    }

    fun acceptRecording(): Boolean {
        if (_state.value !is RecordingState.AwaitingConfirmation) return false
        save()
        reset()
        _state.value = RecordingState.Idle
        macroSupplier.refresh()
        return true
    }

    override fun update(command: Command) {
        list.add(command)
        _state.value = RecordingState.Recording(list.size)
    }

    fun deleteLastAction(): Boolean {
        if (list.isEmpty() || _state.value is RecordingState.Idle) return false
        list.removeAt(list.lastIndex)
        _state.value = when (_state.value) {
            is RecordingState.AwaitingConfirmation ->
                if (list.isEmpty() && recordedStartingPoint !is MacroStartingPoint.App) {
                    reset()
                    RecordingState.Idle
                } else {
                    RecordingState.AwaitingConfirmation(list.toList(), recordedStartingPoint)
                }

            else -> RecordingState.Recording(list.size)
        }
        return true
    }

    fun hasRecordedActions(): Boolean = list.isNotEmpty()

    fun isRecording(): Boolean = _state.value is RecordingState.Recording

    fun isAwaitingConfirmation(): Boolean = _state.value is RecordingState.AwaitingConfirmation

    private fun save() {
        macroRepository.saveRecordedMacro(
            commandSequence = list.toList(),
            startingPoint = recordedStartingPoint,
        )
    }

    private fun reset() {
        list.clear()
        recordedStartingPoint = null
    }

    private fun detectCurrentStartingPoint(): MacroStartingPoint? {
        val pkg =
            runCatching { mediatorProvider.get().mediatorState.value.overlayContext.packageName }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: CommandExecutorService.instance?.rootInActiveWindow?.packageName?.toString()
                    ?.takeIf { it.isNotBlank() }
                ?: return null

        return if (pkg in getLauncherPackages()) {
            MacroStartingPoint.Home
        } else {
            MacroStartingPoint.App(pkg, sectionDetector.currentSection())
        }
    }

    private fun getLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    private companion object {
        const val TAG = "MacroRecorder"
    }
}
