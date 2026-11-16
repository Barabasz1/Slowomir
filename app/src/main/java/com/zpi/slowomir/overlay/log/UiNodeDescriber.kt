package com.zpi.slowomir.overlay.log

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.services.CommandExecutorService
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiNodeDescriber @Inject constructor() {

    private class PathEntry(val node: AccessibilityNodeInfo, val indexInParent: Int)

    fun describeTapAt(x: Float, y: Float): ActionDescriptor? {
        if (!ActionLogConfig.isEnabled) return null

        return try {
            resolve(x.toInt(), y.toInt())
        } catch (e: Exception) {
            Log.w(TAG, "Could not describe the element at ($x, $y)", e)
            null
        }
    }

    private fun resolve(x: Int, y: Int): ActionDescriptor? {
        val service = CommandExecutorService.instance ?: return null
        val root = findRootAt(service, x, y) ?: return null

        val chain = ArrayList<PathEntry>()
        chain.add(PathEntry(root, 0))
        descend(root, x, y, chain)

        val clickableIndex = chain.indexOfLast { isClickable(it.node) }
        val targetIndex = when {
            clickableIndex >= 0 -> clickableIndex
            LOG_NON_CLICKABLE_TARGETS -> chain.lastIndex
            else -> return null
        }
        val target = chain[targetIndex].node

        val packageName = chain.subList(0, targetIndex + 1).asReversed()
            .firstNotNullOfOrNull { it.node.packageName?.toString()?.takeIf { name -> name.isNotBlank() } }
            ?: return null
        if (packageName == service.packageName) return null

        val path = HierarchyPath.format(
            chain.subList(0, targetIndex + 1)
                .map { HierarchyPath.segment(it.node.className, it.indexInParent) }
        )
        val label = labelOf(target) ?: descendantLabel(target)

        return ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = packageName,
            resourceId = target.viewIdResourceName?.takeIf { it.isNotBlank() },
            hierarchyPath = path.takeIf { it.isNotEmpty() },
            labelHash = label?.let { ActionHasher.sha256Hex(it) },
        )
    }

    private fun findRootAt(service: AccessibilityService, x: Int, y: Int): AccessibilityNodeInfo? {
        val ownPackage = service.packageName
        val bounds = Rect()

        val active = service.rootInActiveWindow
        if (active != null && active.packageName?.toString() != ownPackage) {
            active.getBoundsInScreen(bounds)
            if (bounds.contains(x, y)) return active
        }

        return service.windows
            .sortedByDescending { it.layer }
            .firstNotNullOfOrNull { window ->
                window.getBoundsInScreen(bounds)
                val windowRoot = window.root
                if (bounds.contains(x, y) &&
                    windowRoot != null &&
                    windowRoot.packageName?.toString() != ownPackage
                ) windowRoot else null
            }
    }

    private fun descend(
        parent: AccessibilityNodeInfo,
        x: Int,
        y: Int,
        chain: MutableList<PathEntry>,
    ) {
        val bounds = Rect()
        var current = parent
        var depth = 0
        while (depth < MAX_DEPTH) {
            var next: PathEntry? = null
            for (i in current.childCount - 1 downTo 0) {
                val child = current.getChild(i) ?: continue
                if (!child.isVisibleToUser) continue
                child.getBoundsInScreen(bounds)
                if (bounds.contains(x, y)) {
                    next = PathEntry(child, i)
                    break
                }
            }
            if (next == null) return
            chain.add(next)
            current = next.node
            depth++
        }
    }

    private fun isClickable(node: AccessibilityNodeInfo): Boolean =
        node.isClickable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

    private fun labelOf(node: AccessibilityNodeInfo): String? {
        if (node.isPassword) return null
        val candidates = if (node.isEditable) {
            listOf(node.hintText, node.contentDescription)
        } else {
            listOf(node.text, node.contentDescription)
        }
        return candidates.firstNotNullOfOrNull { normalize(it) }
    }

    private fun normalize(value: CharSequence?): String? =
        value?.toString()
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.take(MAX_LABEL_LENGTH)
            ?.takeIf { it.isNotEmpty() }

    private fun descendantLabel(root: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_LABEL_SCAN_NODES) {
            val (node, depth) = queue.removeFirst()
            if (depth >= MAX_LABEL_SCAN_DEPTH) continue
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (!child.isVisibleToUser) continue
                visited++
                labelOf(child)?.let { return it }
                queue.add(child to depth + 1)
            }
        }
        return null
    }

    companion object {
        private const val TAG = "UiNodeDescriber"
        private const val MAX_DEPTH = 80
        private const val MAX_LABEL_LENGTH = 100
        private const val MAX_LABEL_SCAN_DEPTH = 3
        private const val MAX_LABEL_SCAN_NODES = 40
        private const val LOG_NON_CLICKABLE_TARGETS = true
    }
}