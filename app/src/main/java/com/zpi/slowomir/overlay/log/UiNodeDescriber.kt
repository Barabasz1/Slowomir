package com.zpi.slowomir.overlay.log

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.zpi.slowomir.overlay.macro.MacroPlaybackState
import com.zpi.slowomir.overlay.services.CommandExecutorService
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiNodeDescriber @Inject constructor(
    private val playbackState: MacroPlaybackState,
) {

    private class PathEntry(val node: AccessibilityNodeInfo, val indexInParent: Int)

    fun describeTapAt(x: Float, y: Float): ActionDescriptor? {
        if (!ActionLogConfig.isEnabled || playbackState.isPlaying) return null

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
        return resolveInRoot(root, x, y)
    }

    internal fun resolveInRoot(
        root: AccessibilityNodeInfo,
        x: Int,
        y: Int,
    ): ActionDescriptor? {
        val chain = findBestTargetChain(root, x, y) ?: return null
        val target = chain.last().node

        val packageName = chain.asReversed()
            .firstNotNullOfOrNull { it.node.packageName?.toString()?.takeIf { name -> name.isNotBlank() } }
            ?: return null

        val path = HierarchyPath.format(
            chain.map { HierarchyPath.segment(it.node.className, it.indexInParent) }
        )
        val label = labelOf(target, lowercase = false) ?: descendantLabel(target, lowercase = false)

        return ActionDescriptor(
            type = ActionType.UI_CLICK,
            packageName = packageName,
            resourceId = target.viewIdResourceName?.takeIf { it.isNotBlank() },
            hierarchyPath = path.takeIf { it.isNotEmpty() },
            labelHash = label?.let { ActionHasher.sha256Hex(it.lowercase(Locale.ROOT)) },
            label = label,
        )
    }

    private fun findRootAt(service: AccessibilityService, x: Int, y: Int): AccessibilityNodeInfo? {
        val ownPackage = service.packageName
        val bounds = Rect()

        val windowHit = service.windows
            .sortedByDescending { it.layer }
            .firstNotNullOfOrNull { window ->
                window.getBoundsInScreen(bounds)
                val windowRoot = window.root
                val isOwnOverlay = windowRoot?.packageName?.toString() == ownPackage &&
                        window.type != AccessibilityWindowInfo.TYPE_APPLICATION
                if (bounds.contains(x, y) && windowRoot != null && !isOwnOverlay) windowRoot else null
            }
        if (windowHit != null) return windowHit

        val active = service.rootInActiveWindow
        if (active != null) {
            active.getBoundsInScreen(bounds)
            if (bounds.contains(x, y)) return active
        }

        return null
    }

    private fun findBestTargetChain(root: AccessibilityNodeInfo, x: Int, y: Int): List<PathEntry>? {
        val bounds = Rect()
        var bestClickable: List<PathEntry>? = null
        var bestClickableArea = Long.MAX_VALUE
        var bestClickableEnabled = false
        var bestFallback: List<PathEntry>? = null
        var bestFallbackArea = Long.MAX_VALUE
        val currentChain = ArrayList<PathEntry>()

        fun dfs(node: AccessibilityNodeInfo, indexInParent: Int, depth: Int) {
            node.getBoundsInScreen(bounds)
            val containsPoint = x >= bounds.left && x < bounds.right && y >= bounds.top && y < bounds.bottom
            if (!containsPoint && depth > 0) return

            currentChain.add(PathEntry(node, indexInParent))
            if (containsPoint) {
                val width = (bounds.right - bounds.left).coerceAtLeast(0).toLong()
                val height = (bounds.bottom - bounds.top).coerceAtLeast(0).toLong()
                val nodeArea = width * height

                if (isClickable(node)) {
                    val enabled = node.isEnabled
                    if (bestClickable == null ||
                        (enabled && !bestClickableEnabled) ||
                        (enabled == bestClickableEnabled && (
                                nodeArea < bestClickableArea ||
                                        (nodeArea == bestClickableArea && currentChain.size > bestClickable!!.size)
                                ))
                    ) {
                        bestClickable = currentChain.toList()
                        bestClickableArea = nodeArea
                        bestClickableEnabled = enabled
                    }
                } else if (LOG_NON_CLICKABLE_TARGETS) {
                    if (bestFallback == null ||
                        nodeArea < bestFallbackArea ||
                        (nodeArea == bestFallbackArea && currentChain.size > bestFallback!!.size)
                    ) {
                        bestFallback = currentChain.toList()
                        bestFallbackArea = nodeArea
                    }
                }
            }

            if (depth < MAX_DEPTH) {
                for (i in node.childCount - 1 downTo 0) {
                    val child = node.getChild(i) ?: continue
                    if (!child.isVisibleToUser) continue
                    dfs(child, i, depth + 1)
                }
            }

            currentChain.removeAt(currentChain.lastIndex)
        }

        dfs(root, 0, 0)
        return bestClickable ?: bestFallback
    }

    companion object {
        private const val TAG = "UiNodeDescriber"
        private const val MAX_DEPTH = 80
        private const val MAX_LABEL_LENGTH = 100
        private const val MAX_LABEL_SCAN_DEPTH = 3
        private const val MAX_LABEL_SCAN_NODES = 40
        private const val LOG_NON_CLICKABLE_TARGETS = true

        internal fun isClickable(node: AccessibilityNodeInfo): Boolean =
            node.isClickable || node.actionList.orEmpty().any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

        internal fun labelOf(node: AccessibilityNodeInfo, lowercase: Boolean = true): String? {
            if (node.isPassword) return null
            val candidates = if (node.isEditable) {
                listOf(node.hintText, node.contentDescription)
            } else {
                listOf(node.text, node.contentDescription)
            }
            return candidates.firstNotNullOfOrNull { normalizeLabel(it, lowercase) }
        }

        internal fun normalizeLabel(value: CharSequence?, lowercase: Boolean = true): String? =
            value?.toString()
                ?.trim()
                ?.take(MAX_LABEL_LENGTH)
                ?.let { if (lowercase) it.lowercase(Locale.ROOT) else it }
                ?.takeIf { it.isNotEmpty() }

        internal fun descendantLabel(root: AccessibilityNodeInfo, lowercase: Boolean = true): String? {
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
                    labelOf(child, lowercase)?.let { return it }
                    queue.add(child to depth + 1)
                }
            }
            return null
        }
    }
}