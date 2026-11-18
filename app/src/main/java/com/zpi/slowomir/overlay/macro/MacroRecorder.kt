package com.zpi.slowomir.overlay.macro

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.log.CommandLogger
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.voice.mediator.CommandMediator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class MacroRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandLogger: CommandLogger,
    private val macroRepository: MacroRepository,
    private val macroSupplier: MacroSupplier,
    private val mediatorProvider: Provider<CommandMediator>,
) : CommandLogger.ICommandLoggerListener {

    private val list: MutableList<Command> = mutableListOf()
    private var recordedStartingPoint: MacroStartingPoint? = null

    fun startRecording(): Boolean {
        reset()
        recordedStartingPoint = detectCurrentStartingPoint()
        commandLogger.attachListener(this)
        return true
    }

    fun finishRecording(): Boolean {
        commandLogger.detachListener()
        return true
    }

    fun discardRecording(): Boolean {
        commandLogger.detachListener()
        reset()
        return true
    }

    fun acceptRecording(): Boolean {
        commandLogger.detachListener()
        save()
        reset()
        macroSupplier.refresh()
        return true
    }

    override fun update(command: Command) {
        list.add(command)
    }

    fun isRecordingEmpty(): Boolean {
        return list.isEmpty()
    }

    private fun save() {
        macroRepository.saveRecordedMacro(list.toList(), recordedStartingPoint)
    }

    private fun reset() {
        list.clear()
        recordedStartingPoint = null
    }

    private fun detectCurrentStartingPoint(): MacroStartingPoint? {
        val pkg = runCatching { mediatorProvider.get().mediatorState.value.overlayContext.packageName }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: CommandExecutorService.instance?.rootInActiveWindow?.packageName?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: return null

        return if (pkg in getLauncherPackages()) {
            MacroStartingPoint.Home
        } else {
            MacroStartingPoint.App(pkg)
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
}