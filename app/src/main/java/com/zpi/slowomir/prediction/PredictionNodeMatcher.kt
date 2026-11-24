package com.zpi.slowomir.prediction

import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionDescriptor
import com.zpi.slowomir.overlay.log.ActionHasher
import com.zpi.slowomir.overlay.log.ActionType
import com.zpi.slowomir.overlay.log.HierarchyPath
import com.zpi.slowomir.overlay.log.UiNodeDescriber
import com.zpi.slowomir.overlay.log.db.ActionDefEntity

object PredictionNodeMatcher {

    private const val MAX_NODES = 600

    private data class NodeEntry(
        val node: AccessibilityNodeInfo,
        val segments: List<String>,
        val inheritedPackage: String?,
    )

    fun findMatchingNodes(root: AccessibilityNodeInfo, targetDef: ActionDefEntity): List<AccessibilityNodeInfo> {
        if (targetDef.actionType != ActionType.UI_CLICK) return emptyList()

        val expectedSimpleClass = targetDef.hierarchyPath
            ?.substringAfterLast('/')
            ?.substringBefore('[')
            ?.takeIf { it.isNotEmpty() }

        val rootPackage = root.packageName?.toString()?.takeIf { it.isNotBlank() }
        val queue = ArrayDeque<NodeEntry>()
        queue.add(
            NodeEntry(
                node = root,
                segments = listOf(HierarchyPath.segment(root.className, 0)),
                inheritedPackage = rootPackage,
            )
        )

        val exactMatches = mutableListOf<AccessibilityNodeInfo>()
        val fallbackMatches = mutableListOf<AccessibilityNodeInfo>()
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val entry = queue.removeFirst()
            val node = entry.node
            visited++

            val pkg = node.packageName?.toString()?.takeIf { it.isNotBlank() } ?: entry.inheritedPackage

            if (node.isVisibleToUser &&
                !node.isPassword &&
                !node.isEditable &&
                !PredictionGuard.isGuardedNode(node)
            ) {
                val pkgMatches = targetDef.packageName == null || pkg == targetDef.packageName
                val resId = node.viewIdResourceName?.takeIf { it.isNotBlank() }
                val resMatches = resId == targetDef.resourceId
                val path = HierarchyPath.format(entry.segments).takeIf { it.isNotEmpty() }
                val pathMatches = path == targetDef.hierarchyPath

                val nodeSimpleClass = node.className
                    ?.toString()
                    ?.substringAfterLast('.')
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val classMatches = expectedSimpleClass == null || nodeSimpleClass == expectedSimpleClass

                val canExact = exactMatches.size < 2 && pkgMatches && resMatches && pathMatches
                val canFallback = fallbackMatches.size < 2 &&
                    targetDef.labelHash != null &&
                    pkgMatches &&
                    resMatches &&
                    classMatches &&
                    UiNodeDescriber.isClickable(node)

                if (canExact || canFallback) {
                    val label = UiNodeDescriber.labelOf(node) ?: UiNodeDescriber.descendantLabel(node)
                    val isGuardedLabel = label != null &&
                        PredictionGuard.isProtectedLabel(PredictionGuard.normalizeText(label))

                    if (!isGuardedLabel) {
                        val labelHash = label?.let { ActionHasher.sha256Hex(it) }

                        if (canExact) {
                            val descriptor = ActionDescriptor(
                                type = ActionType.UI_CLICK,
                                packageName = pkg,
                                resourceId = resId,
                                hierarchyPath = path,
                                labelHash = labelHash,
                            )
                            if (ActionHasher.hash(descriptor) == targetDef.actionHash) {
                                exactMatches.add(node)
                                if (exactMatches.size >= 2) {
                                    return exactMatches
                                }
                            }
                        }

                        if (canFallback && labelHash == targetDef.labelHash) {
                            fallbackMatches.add(node)
                        }
                    }
                }
            }

            if (!node.isPassword) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    if (child.isVisibleToUser) {
                        val nextSegments = (entry.segments + HierarchyPath.segment(child.className, i))
                            .takeLast(HierarchyPath.MAX_SEGMENTS)
                        queue.add(NodeEntry(child, nextSegments, pkg))
                    }
                }
            }
        }

        return if (exactMatches.isNotEmpty()) exactMatches else fallbackMatches
    }
}
