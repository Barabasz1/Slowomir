package com.zpi.slowomir.overlay.log

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandFactory
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.voice.mediator.CommandMediator
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class CommandLogger @Inject constructor(
    private val actionRecorder: ActionRecorder,
    private val commandFactory: CommandFactory,
    private val mediatorProvider: Provider<CommandMediator>,
) : ICommandLogger {
    interface ICommandLoggerListener {
        fun update(command: Command)
    }

    private var listener: ICommandLoggerListener? = null

    override fun log(command: Command) {
        if (!ActionLogConfig.shouldLog(command)) return

        notifyListener(command)
        save(command)
    }

    private fun save(command: Command) {
        val serialized = commandFactory.serialize(command)
        val currentPackage = resolveCurrentPackage()
        val logged = (command as? ActionLoggable)?.consumeLoggedAction()

        val descriptor = if (logged != null) {
            logged.copy(
                packageName = logged.packageName ?: currentPackage,
                commandType = serialized.type,
                commandArgs = serialized.argumentsJson,
            )
        } else {
            ActionDescriptor(
                type = ActionType.COMMAND,
                packageName = currentPackage,
                commandType = serialized.type,
                commandArgs = serialized.argumentsJson,
            )
        }

        actionRecorder.record(descriptor, System.currentTimeMillis())
    }

    private fun resolveCurrentPackage(): String? {
        val fromMediator = runCatching {
            mediatorProvider.get().mediatorState.value.overlayContext.packageName
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return fromMediator
            ?: CommandExecutorService.instance?.rootInActiveWindow?.packageName?.toString()
                ?.takeIf { it.isNotBlank() }
    }

    fun attachListener(listener: ICommandLoggerListener) {
        this.listener = listener
    }

    fun detachListener() {
        this.listener = null
    }

    private fun notifyListener(command: Command) {
        this.listener?.update(command)
    }
}