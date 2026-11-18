package com.zpi.slowomir.overlay.macro

import android.content.Context
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgumentCodec
import com.zpi.slowomir.overlay.commands.CommandFactory
import com.zpi.slowomir.overlay.commands.SerializedCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SerializedMacro(
    val startingPoint: MacroStartingPoint?,
    val commands: List<SerializedCommand>,
)

@Singleton
class MacroRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandFactory: CommandFactory,
    private val macroStatisticalGenerator: MacroStatisticalGenerator,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveRecordedMacro(
        commandSequence: List<Command>,
        startingPoint: MacroStartingPoint? = null,
    ) {
        if (commandSequence.isEmpty()) return

        val existing = readStoredArray()
        val macroObj = JSONObject().apply {
            put(KEY_STARTING_POINT, encodeStartingPoint(startingPoint))
            val commandsArray = JSONArray()
            for (command in commandSequence) {
                val serialized = commandFactory.serialize(command)
                val cmdObj = JSONObject().apply {
                    put(KEY_CMD_TYPE, serialized.type)
                    put(KEY_CMD_NAME, serialized.name)
                    put(KEY_CMD_ARGS, serialized.argumentsJson)
                    put(KEY_CMD_DISPLAY, serialized.displayText)
                }
                commandsArray.put(cmdObj)
            }
            put(KEY_COMMANDS, commandsArray)
        }

        val updated = JSONArray()
        updated.put(macroObj)
        for (i in 0 until existing.length()) {
            updated.put(existing.getJSONObject(i))
        }
        prefs.edit().putString(KEY_RECORDED_MACROS, updated.toString()).apply()
    }

    @Synchronized
    fun getRecordedMacros(limit: Int = 10): List<Macro> {
        return getRecordedMacroDetails(limit).mapNotNull { stored ->
            val commands = stored.commands.mapNotNull { commandFactory.create(it) }
            if (commands.isEmpty()) return@mapNotNull null
            Macro(
                commands = commands,
                startingPoint = stored.startingPoint,
                serializedCommands = stored.commands,
            )
        }
    }

    @Synchronized
    fun getRecordedMacroDetails(limit: Int = 10): List<SerializedMacro> {
        val array = readStoredArray()
        val count = minOf(limit, array.length())
        val result = ArrayList<SerializedMacro>(count)

        for (i in 0 until count) {
            val macroObj = array.optJSONObject(i) ?: continue
            val startingPoint = decodeStartingPoint(macroObj.optJSONObject(KEY_STARTING_POINT))
            val commandsArray = macroObj.optJSONArray(KEY_COMMANDS) ?: continue
            val commands = ArrayList<SerializedCommand>(commandsArray.length())

            for (j in 0 until commandsArray.length()) {
                val cmdObj = commandsArray.optJSONObject(j) ?: continue
                val type = cmdObj.optString(KEY_CMD_TYPE).takeIf { it.isNotBlank() } ?: continue
                val name = cmdObj.optString(KEY_CMD_NAME).ifBlank { type.substringAfterLast('.') }
                val argsJson = cmdObj.optString(KEY_CMD_ARGS)
                val args = CommandArgumentCodec.decode(argsJson)
                val display = cmdObj.optString(KEY_CMD_DISPLAY).ifBlank { name }
                commands.add(
                    SerializedCommand(
                        type = type,
                        name = name,
                        arguments = args,
                        displayText = display,
                    )
                )
            }

            if (commands.isNotEmpty()) {
                result.add(
                    SerializedMacro(
                        startingPoint = startingPoint,
                        commands = commands,
                    )
                )
            }
        }
        return result
    }

    @Synchronized
    fun deleteRecordedMacro(index: Int): Boolean {
        val existing = readStoredArray()
        if (index !in 0 until existing.length()) return false
        val updated = JSONArray()
        for (i in 0 until existing.length()) {
            if (i != index) updated.put(existing.getJSONObject(i))
        }
        prefs.edit().putString(KEY_RECORDED_MACROS, updated.toString()).apply()
        return true
    }

    @Synchronized
    fun clearRecordedMacros() {
        prefs.edit().remove(KEY_RECORDED_MACROS).apply()
    }

    fun getGeneratedMacros(
        currentPackageName: String? = null,
        limit: Int = 5,
    ): List<Macro> {
        return runBlocking(Dispatchers.IO) {
            macroStatisticalGenerator.generateMacros(
                currentPackageName = currentPackageName,
                limit = limit,
            )
        }
    }

    suspend fun fetchGeneratedMacros(
        currentPackageName: String? = null,
        limit: Int = 5,
    ): List<Macro> {
        return macroStatisticalGenerator.generateMacros(
            currentPackageName = currentPackageName,
            limit = limit,
        )
    }

    private fun readStoredArray(): JSONArray {
        val raw = prefs.getString(KEY_RECORDED_MACROS, null) ?: return JSONArray()
        return runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
    }

    private fun encodeStartingPoint(startingPoint: MacroStartingPoint?): Any {
        return when (startingPoint) {
            null -> JSONObject.NULL
            is MacroStartingPoint.Home -> JSONObject().apply {
                put(KEY_SP_TYPE, SP_TYPE_HOME)
            }
            is MacroStartingPoint.App -> JSONObject().apply {
                put(KEY_SP_TYPE, SP_TYPE_APP)
                put(KEY_SP_APP_NAME, startingPoint.appName)
            }
        }
    }

    private fun decodeStartingPoint(obj: JSONObject?): MacroStartingPoint? {
        if (obj == null) return null
        return when (obj.optString(KEY_SP_TYPE)) {
            SP_TYPE_HOME -> MacroStartingPoint.Home
            SP_TYPE_APP -> obj.optString(KEY_SP_APP_NAME)
                .takeIf { it.isNotBlank() }
                ?.let { MacroStartingPoint.App(it) }
            else -> null
        }
    }

    companion object {
        private const val PREFS_NAME = "slowomir_recorded_macros"
        private const val KEY_RECORDED_MACROS = "recorded_macros"
        private const val KEY_STARTING_POINT = "startingPoint"
        private const val KEY_COMMANDS = "commands"
        private const val KEY_CMD_TYPE = "type"
        private const val KEY_CMD_NAME = "name"
        private const val KEY_CMD_ARGS = "arguments"
        private const val KEY_CMD_DISPLAY = "displayText"
        private const val KEY_SP_TYPE = "type"
        private const val KEY_SP_APP_NAME = "appName"
        private const val SP_TYPE_HOME = "home"
        private const val SP_TYPE_APP = "app"
    }
}