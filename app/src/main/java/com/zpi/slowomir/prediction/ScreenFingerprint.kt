package com.zpi.slowomir.prediction

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.zpi.slowomir.overlay.log.ActionHasher

object ScreenFingerprint {

    const val UNKNOWN_SCREEN = "UNKNOWN_SCREEN"

    fun compute(root: AccessibilityNodeInfo?, screenWidth: Int = 1080, screenHeight: Int = 2400): String {
        if (root == null) return UNKNOWN_SCREEN

        val identifiers = mutableSetOf<String>()
        val bounds = Rect()

        // 1. Check paneTitle if available (often set by Compose navigation destinations)
        root.paneTitle?.toString()?.takeIf { it.isNotBlank() }?.let {
            identifiers.add("PANE:$it")
        }

        val topBarThreshold = (screenHeight * 0.15f).toInt()
        val bottomBarThreshold = (screenHeight * 0.85f).toInt()

        // BFS / DFS on the active tree
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 60

        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++

            node.getBoundsInScreen(bounds)

            // Check paneTitle on sub-containers
            node.paneTitle?.toString()?.takeIf { it.isNotBlank() }?.let {
                identifiers.add("PANE:$it")
            }

            // Only consider elements located in the top bar or bottom bar areas
            val inTopBar = bounds.top >= 0 && bounds.bottom <= topBarThreshold
            val inBottomBar = bounds.top >= bottomBarThreshold && bounds.bottom <= screenHeight

            if (inTopBar || inBottomBar) {
                // Collect stable resourceId
                node.viewIdResourceName?.takeIf { it.isNotBlank() }?.let { resId ->
                    identifiers.add("RES:$resId")
                }

                // If no resourceId, collect stable contentDescription or text (Compose / custom)
                val label = (node.contentDescription ?: node.text)?.toString()?.trim()
                if (!label.isNullOrBlank() && label.length <= 40 && !label.any { it.isDigit() }) {
                    identifiers.add("LABEL:${node.className}:$label")
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (child.isVisibleToUser) {
                    queue.add(child)
                }
            }
        }

        if (identifiers.size < PredictionConfig.MIN_SCREEN_IDS) {
            return UNKNOWN_SCREEN
        }

        val sorted = identifiers.sorted().joinToString(";")
        return ActionHasher.sha256Hex(sorted)
    }
}
