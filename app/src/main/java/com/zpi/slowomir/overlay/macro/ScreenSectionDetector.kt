package com.zpi.slowomir.overlay.macro

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.services.CommandExecutorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenSectionDetector @Inject constructor() {

    fun currentSection(): String? {
        val root = CommandExecutorService.instance?.rootInActiveWindow ?: return null
        return runCatching { findSelectedTab(root) }
            .onFailure { Log.w(TAG, "Could not read the current section", it) }
            .getOrNull()
    }

    fun open(section: String): Boolean {
        val service = CommandExecutorService.instance ?: return false
        val root = service.rootInActiveWindow ?: return false
        val tab = root.findAccessibilityNodeInfosByText(section)
            .firstOrNull { label(it).equals(section, ignoreCase = true) }
        if (tab == null) {
            Log.w(TAG, "Section '$section' not found on the screen")
            return false
        }
        return service.touchExecutor?.tapNode(tab) ?: false
    }

    private fun findSelectedTab(root: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (node.isSelected && !node.isEditable && !node.isCheckable && isTabLike(node)) {
                label(node)?.let { return it }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }

    private fun isTabLike(node: AccessibilityNodeInfo): Boolean =
        node.isClickable || node.className?.contains("Tab", ignoreCase = true) == true

    private fun label(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: node.getChild(0)?.let { child -> child.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } }
        return text?.take(MAX_LABEL_LENGTH)
    }

    private companion object {
        const val TAG = "ScreenSectionDetector"
        const val MAX_NODES = 600
        const val MAX_LABEL_LENGTH = 40
    }
}
