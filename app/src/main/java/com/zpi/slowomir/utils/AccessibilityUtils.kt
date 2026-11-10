package com.zpi.slowomir.utils

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Utility functions for traversing and querying the Android Accessibility hierarchy.
 */
object AccessibilityUtils {

    /**
     * Recursively traverses the view hierarchy starting at [root] and returns the first node
     * that matches the specified [predicate], or `null` if no match is found.
     *
     * @param root The root [AccessibilityNodeInfo] to search from.
     * @param predicate The condition to test each node against.
     * @return The first matching [AccessibilityNodeInfo], or `null`.
     */
    fun findNode(
        root: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        if (predicate(root)) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findNode(child, predicate)?.let { return it }
        }
        return null
    }

    /**
     * Determines whether an [AccessibilityNodeInfo] represents an editable text input candidate.
     *
     * @param node The node to evaluate.
     * @return `true` if the node is editable or supports text input actions.
     */
    fun isInputCandidate(node: AccessibilityNodeInfo): Boolean {
        val hasSetTextAction = node.actionList?.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT } == true
        val isEditTextClass = node.className?.contains("EditText", ignoreCase = true) == true
        return node.isEditable || hasSetTextAction || isEditTextClass
    }

    /**
     * Finds the currently focused or active text input node in the active window of the given [service].
     *
     * @param service The accessibility service used to inspect windows and focus.
     * @return The active [AccessibilityNodeInfo] suitable for text input, or `null` if none is found.
     */
    fun findActiveInputNode(service: AccessibilityService): AccessibilityNodeInfo? {
        service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { return it }

        val root = service.rootInActiveWindow ?: return null
        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { return it }

        return findNode(root) { it.isFocused && isInputCandidate(it) } ?: if (root.isFocused) root else null
    }
}
