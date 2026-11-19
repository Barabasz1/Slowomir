package com.zpi.slowomir.overlay.ui.common

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.zpi.slowomir.overlay.services.CommandExecutorService
import com.zpi.slowomir.overlay.services.ScreenEventsBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UiNode(
    val bounds: ScreenRect,
    val className: String? = null,
    val viewId: String? = null,
    val text: String? = null,
    val description: String? = null,
    val clickable: Boolean = false,
    val longClickable: Boolean = false,
    val editable: Boolean = false,
    val checkable: Boolean = false,
    val scrollable: Boolean = false,
    val focused: Boolean = false,
    val enabled: Boolean = true,
) {
    val interactive: Boolean get() = clickable || longClickable || editable || checkable
}

/**
 * Everything read from the screen in one walk of the accessibility tree.
 *
 * @param nodes Visible nodes of the active window.
 * @param focusedField Bounds of the field with input focus.
 * @param keyboard Bounds of the keyboard window if visible.
 */
data class UiSnapshot(
    val nodes: List<UiNode> = emptyList(),
    val focusedField: ScreenRect? = null,
    val keyboard: ScreenRect? = null,
)

interface UiTreeSource {
    fun snapshot(): UiSnapshot

    val changes: Flow<Unit>
}

/**
 * [UiTreeSource] creates cached snapshot of accessibility tree nodes.
 * Returns empty snapshot when accessibility service is not enabled.
 */
@Singleton
class AccessibilityUiTreeSource @Inject constructor(screenEventsBus: ScreenEventsBus) : UiTreeSource {

    override val changes: Flow<Unit> = screenEventsBus.types
        .filter { it in LAYOUT_EVENTS }
        .map { }

    private var cached = UiSnapshot()
    private var cachedAt = 0L

    @Synchronized
    override fun snapshot(): UiSnapshot {
        val now = System.nanoTime()
        if (cachedAt != 0L && now - cachedAt < CACHE_MS * 1_000_000) return cached
        cached = CommandExecutorService.instance?.let(::read) ?: UiSnapshot()
        cachedAt = now
        return cached
    }

    private fun read(service: AccessibilityService): UiSnapshot {
        val bounds = Rect()
        fun rect() = ScreenRect(bounds.left, bounds.top, bounds.right, bounds.bottom)

        val nodes = mutableListOf<UiNode>()
        runCatching {
            val root = service.rootInActiveWindow ?: return@runCatching
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            var visited = 0
            while (queue.isNotEmpty() && visited < MAX_NODES) {
                val node = queue.removeFirst()
                visited++
                for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)

                if (!node.isVisibleToUser) continue
                node.getBoundsInScreen(bounds)
                if (bounds.isEmpty) continue
                nodes += UiNode(
                    bounds = rect(),
                    className = node.className?.toString(),
                    viewId = node.viewIdResourceName,
                    text = node.text?.toString(),
                    description = node.contentDescription?.toString(),
                    clickable = node.isClickable,
                    longClickable = node.isLongClickable,
                    editable = node.isEditable,
                    checkable = node.isCheckable,
                    scrollable = node.isScrollable,
                    focused = node.isFocused,
                    enabled = node.isEnabled,
                )
            }
        }

        val focusedField = runCatching {
            service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { node ->
                node.getBoundsInScreen(bounds)
                rect().takeIf { !it.isEmpty }
            }
        }.getOrNull()

        val keyboard = runCatching {
            service.windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }?.let { window ->
                window.getBoundsInScreen(bounds)
                rect().takeIf { !it.isEmpty }
            }
        }.getOrNull()

        return UiSnapshot(nodes, focusedField, keyboard)
    }

    private companion object {
        const val CACHE_MS = 250L
        const val MAX_NODES = 500

        val LAYOUT_EVENTS = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
        )
    }
}
