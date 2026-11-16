package com.zpi.slowomir.overlay.log

enum class ActionType {
    UI_CLICK,
    HOME,
    BACK,
    LAST_APPS,
}

data class ActionDescriptor(
    val type: ActionType,
    val packageName: String? = null,
    val resourceId: String? = null,
    val hierarchyPath: String? = null,
    val labelHash: String? = null,
) {
    companion object {
        fun system(type: ActionType): ActionDescriptor {
            require(type != ActionType.UI_CLICK) { "Use ActionDescriptor(UI_CLICK, ...) for UI clicks" }
            return ActionDescriptor(type)
        }
    }
}

interface ActionLoggable {
    fun consumeLoggedAction(): ActionDescriptor?
}
