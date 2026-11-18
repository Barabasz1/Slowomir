package com.zpi.slowomir.overlay.log

enum class ActionType {
    UI_CLICK,
    HOME,
    BACK,
    LAST_APPS,
    COMMAND,
}

data class ActionDescriptor(
    val type: ActionType,
    val packageName: String? = null,
    val resourceId: String? = null,
    val hierarchyPath: String? = null,
    val labelHash: String? = null,
    val commandType: String? = null,
    val commandArgs: String? = null,
) {
    companion object {
        fun system(type: ActionType): ActionDescriptor {
            require(type != ActionType.UI_CLICK && type != ActionType.COMMAND) {
                "Use ActionDescriptor(...) directly for UI_CLICK and COMMAND"
            }
            return ActionDescriptor(type)
        }
    }
}

interface ActionLoggable {
    fun consumeLoggedAction(): ActionDescriptor?
}