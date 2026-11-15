package com.zpi.slowomir.utils

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Utility functions for traversing and querying the Android Accessibility hierarchy.
 */
object AccessibilityUtils {
    private const val TAG = "AccessibilityUtils"

    /**
     * Finds a node in the accessibility hierarchy that matches the given predicate.
     *
     * @param root The root node to start the search from.
     * @param predicate A function that takes an AccessibilityNodeInfo and returns a Boolean.
     * @return The first node that matches the predicate, or null if no match is found.
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
     * Checks if a node is an input candidate for user input.
     *
     * @param node The node to check.
     * @return True if the node is an input candidate, false otherwise.
     */
    fun isInputCandidate(node: AccessibilityNodeInfo): Boolean {
        val hasSetTextAction = node.actionList?.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT } == true
        val isEditTextClass = node.className?.contains("EditText", ignoreCase = true) == true
        return node.isEditable || hasSetTextAction || isEditTextClass
    }

    /**
     * Finds the active input node in the accessibility hierarchy.
     *
     * @param service The accessibility service to query.
     * @return The active input node, or null if no match is found.
     */
    fun findActiveInputNode(service: AccessibilityService): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "Root in active window is null")
            return null
        }

        // 1. Try to find a node that is both focused and an input candidate
        val focusedCandidate = findNode(root) { it.isFocused && isInputCandidate(it) }
        if (focusedCandidate != null) {
            Log.i(TAG, "Found focused input candidate: ${focusedCandidate.className}")
            return focusedCandidate
        }

        // 2. Check if the system thinks there's a focused input node and if it's a valid candidate
        val systemFocused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (systemFocused != null) {
            if (isInputCandidate(systemFocused)) {
                Log.i(TAG, "Found system focused candidate: ${systemFocused.className}")
                return systemFocused
            }
            
            // Sometimes the system focused node is a wrapper so its children are worth-checking
            val childCandidate = findNode(systemFocused) { isInputCandidate(it) }
            if (childCandidate != null) {
                Log.i(TAG, "Found input candidate inside system focused node: ${childCandidate.className}")
                return childCandidate
            }
        }

        // 3. Fallback: Find any input candidate on the screen (only safe if there's only one, but better than nothing)
        val anyCandidate = findNode(root) { isInputCandidate(it) }
        if (anyCandidate != null) {
            Log.i(TAG, "Fallback to first available input candidate: ${anyCandidate.className}")
            return anyCandidate
        }

        Log.w(TAG, "No input candidate found.")
        return null
    }
}
