package com.zpi.slowomir.overlay.macro

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.zpi.slowomir.overlay.log.db.ActionDao
import com.zpi.slowomir.overlay.log.db.LoggedActionEvent
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandFactory
import com.zpi.slowomir.overlay.commands.navigation.NavigateHomeCommand
import com.zpi.slowomir.overlay.commands.navigation.OpenAppCommand
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.voice.mediator.CommandMediator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class MacroStatisticalGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val actionDao: ActionDao,
    private val commandFactory: CommandFactory,
    private val mediatorProvider: Provider<CommandMediator>,
) {
    private sealed interface Position {
        data object Home : Position
        data class App(val packageName: String) : Position
    }

    private data class ResolvedEvent(
        val positionBefore: Position,
        val signature: String,
        val command: Command,
    )

    private data class SequenceKey(val start: Position, val signatures: List<String>)

    suspend fun generateMacros(
        currentPackageName: String? = null,
        limit: Int = DEFAULT_LIMIT,
        anyPosition: Boolean = false,
    ): List<Macro> {
        val launcherPackages = getLauncherPackages()
        val targetPosition = resolveCurrentPosition(currentPackageName, launcherPackages)
        val events = actionDao.getAllLoggedEvents()
        if (events.isEmpty()) return emptyList()

        val sessions = splitIntoSessions(events)
        val frequency = linkedMapOf<SequenceKey, Int>()
        val commands = linkedMapOf<SequenceKey, List<Command>>()

        for (session in sessions) {
            val resolvedSession = resolveSessionEvents(session, launcherPackages)
            for (startIdx in resolvedSession.indices) {
                val start = resolvedSession[startIdx].positionBefore
                if (!anyPosition && start != targetPosition) continue

                val maxLen = minOf(MAX_SEQUENCE_LENGTH, resolvedSession.size - startIdx)
                for (len in MIN_SEQUENCE_LENGTH..maxLen) {
                    val slice = resolvedSession.subList(startIdx, startIdx + len)
                    val key = SequenceKey(start, slice.map { it.signature })
                    frequency[key] = (frequency[key] ?: 0) + 1
                    commands.putIfAbsent(key, slice.map { it.command })
                }
            }
        }

        if (frequency.isEmpty()) return emptyList()

        val hasNonTrivial = frequency.keys.any { it.signatures.toSet().size > 1 }
        val ranked = frequency.entries
            .filter { (key, count) ->
                count >= MIN_FREQUENCY && (!hasNonTrivial || key.signatures.toSet().size > 1)
            }
            .sortedWith(
                compareByDescending<Map.Entry<SequenceKey, Int>> { it.value }
                    .thenByDescending { it.key.signatures.size }
            )

        val chosen = mutableListOf<SequenceKey>()
        for ((key, _) in ranked) {
            if (chosen.none { isNearDuplicate(it, key) }) chosen.add(key)
            if (chosen.size == limit) break
        }

        return chosen.mapNotNull { key ->
            val sequence = commands[key] ?: return@mapNotNull null
            Macro(
                commands = sequence,
                startingPoint = when (val start = key.start) {
                    is Position.Home -> MacroStartingPoint.Home
                    is Position.App -> MacroStartingPoint.App(start.packageName)
                },
                serializedCommands = sequence.map { commandFactory.serialize(it) },
            )
        }
    }

    private fun isNearDuplicate(a: SequenceKey, b: SequenceKey): Boolean {
        if (a.start != b.start) return false
        val (short, long) = if (a.signatures.size <= b.signatures.size) a.signatures to b.signatures else b.signatures to a.signatures
        return (0..long.size - short.size).any { offset -> long.subList(offset, offset + short.size) == short }
    }

    private fun resolveSessionEvents(
        session: List<LoggedActionEvent>,
        launcherPackages: Set<String>,
    ): List<ResolvedEvent> {
        val result = ArrayList<ResolvedEvent>(session.size)
        var trackedPosition: Position? = null

        for (event in session) {
            val eventPosition = event.packageName
                ?.takeIf { it.isNotBlank() }
                ?.let { toPosition(it, launcherPackages) }
                ?: trackedPosition
                ?: Position.Home

            val command = reconstructCommand(event) ?: continue
            val signature = buildSignature(event, command)
            result.add(
                ResolvedEvent(
                    positionBefore = eventPosition,
                    signature = signature,
                    command = command,
                )
            )

            trackedPosition = when {
                event.actionType == ActionType.HOME || command is NavigateHomeCommand -> Position.Home
                command is OpenAppCommand -> {
                    val targetPkg = command.arguments["packageName"]?.toString()?.takeIf { it.isNotBlank() }
                    if (targetPkg != null) toPosition(targetPkg, launcherPackages) else eventPosition
                }
                else -> eventPosition
            }
        }

        return result
    }

    private fun reconstructCommand(event: LoggedActionEvent): Command? {
        commandFactory.createFromJson(event.commandType, event.commandArgs)?.let { return it }
        return when (event.actionType) {
            ActionType.HOME -> commandFactory.createNavigateHome()
            ActionType.BACK -> commandFactory.createNavigateBack()
            ActionType.LAST_APPS -> commandFactory.createNavigateRecent()
            ActionType.UI_CLICK -> commandFactory.createTap(0f, 0f)
            ActionType.COMMAND -> null
        }
    }

    private fun buildSignature(event: LoggedActionEvent, command: Command): String {
        return if (event.actionType == ActionType.UI_CLICK &&
            (event.resourceId != null || event.hierarchyPath != null || event.labelHash != null)
        ) {
            "UI_CLICK:${event.packageName}:${event.resourceId}:${event.hierarchyPath}:${event.labelHash}"
        } else {
            val serialized = commandFactory.serialize(command)
            "CMD:${serialized.type}:${serialized.argumentsJson}"
        }
    }

    private fun splitIntoSessions(events: List<LoggedActionEvent>): List<List<LoggedActionEvent>> {
        val sessions = mutableListOf<MutableList<LoggedActionEvent>>()
        var current = mutableListOf<LoggedActionEvent>()

        for (event in events) {
            val prev = current.lastOrNull()
            if (prev != null && event.timestamp - prev.timestamp > SESSION_TIMEOUT_MS) {
                sessions.add(current)
                current = mutableListOf()
            }
            current.add(event)
        }
        if (current.isNotEmpty()) {
            sessions.add(current)
        }
        return sessions
    }

    private fun resolveCurrentPosition(
        explicitPackage: String?,
        launcherPackages: Set<String>,
    ): Position {
        val pkg = explicitPackage?.takeIf { it.isNotBlank() }
            ?: runCatching { mediatorProvider.get().mediatorState.value.overlayContext.packageName }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            ?: CommandExecutorService.instance?.rootInActiveWindow?.packageName?.toString()
                ?.takeIf { it.isNotBlank() }

        return if (pkg == null || pkg in launcherPackages) {
            Position.Home
        } else {
            Position.App(pkg)
        }
    }

    private fun toPosition(packageName: String, launcherPackages: Set<String>): Position {
        return if (packageName in launcherPackages) Position.Home else Position.App(packageName)
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

    companion object {
        private const val MIN_SEQUENCE_LENGTH = 2
        private const val MAX_SEQUENCE_LENGTH = 4
        private const val MIN_FREQUENCY = 1
        private const val SESSION_TIMEOUT_MS = 60_000L
        private const val DEFAULT_LIMIT = 5
    }
}