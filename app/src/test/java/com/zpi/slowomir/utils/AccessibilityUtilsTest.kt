package com.zpi.slowomir.utils

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AccessibilityUtilsTest {

    @Test
    fun `findNode returns null when root is null`() {
        val result = AccessibilityUtils.findNode(null) { true }
        assertNull(result)
    }

    @Test
    fun `findNode returns root when predicate matches root`() {
        val root = mock<AccessibilityNodeInfo>()
        whenever(root.className).thenReturn("android.widget.Button")

        val result = AccessibilityUtils.findNode(root) { it.className == "android.widget.Button" }

        assertEquals(root, result)
    }

    @Test
    fun `findNode traverses children recursively and returns matching child`() {
        val root = mock<AccessibilityNodeInfo>()
        val child1 = mock<AccessibilityNodeInfo>()
        val child2 = mock<AccessibilityNodeInfo>()

        whenever(root.childCount).thenReturn(2)
        whenever(root.getChild(0)).thenReturn(child1)
        whenever(root.getChild(1)).thenReturn(child2)

        whenever(root.className).thenReturn("android.widget.FrameLayout")
        whenever(child1.className).thenReturn("android.widget.TextView")
        whenever(child2.className).thenReturn("android.widget.EditText")

        val result = AccessibilityUtils.findNode(root) { it.className == "android.widget.EditText" }

        assertEquals(child2, result)
    }

    @Test
    fun `findNode returns null when predicate does not match any node`() {
        val root = mock<AccessibilityNodeInfo>()
        val child = mock<AccessibilityNodeInfo>()

        whenever(root.childCount).thenReturn(1)
        whenever(root.getChild(0)).thenReturn(child)

        whenever(root.className).thenReturn("android.widget.FrameLayout")
        whenever(child.className).thenReturn("android.widget.TextView")

        val result = AccessibilityUtils.findNode(root) { it.className == "android.widget.ImageView" }

        assertNull(result)
    }

    @Test
    fun `isInputCandidate returns true when isEditable is true`() {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.isEditable).thenReturn(true)

        assertTrue(AccessibilityUtils.isInputCandidate(node))
    }

    @Test
    fun `isInputCandidate returns true when node has ACTION_SET_TEXT action`() {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.isEditable).thenReturn(false)
        val action = mock<AccessibilityNodeInfo.AccessibilityAction>()
        whenever(action.id).thenReturn(AccessibilityNodeInfo.ACTION_SET_TEXT)
        whenever(node.actionList).thenReturn(listOf(action))

        assertTrue(AccessibilityUtils.isInputCandidate(node))
    }

    @Test
    fun `isInputCandidate returns true when className contains EditText`() {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.isEditable).thenReturn(false)
        whenever(node.actionList).thenReturn(emptyList())
        whenever(node.className).thenReturn("android.widget.EditText")

        assertTrue(AccessibilityUtils.isInputCandidate(node))
    }

    @Test
    fun `isInputCandidate returns false for regular non-input view`() {
        val node = mock<AccessibilityNodeInfo>()
        whenever(node.isEditable).thenReturn(false)
        whenever(node.actionList).thenReturn(emptyList())
        whenever(node.className).thenReturn("android.widget.TextView")

        assertFalse(AccessibilityUtils.isInputCandidate(node))
    }

    @Test
    fun `findActiveInputNode returns focused node from service findFocus`() {
        val service = mock<AccessibilityService>()
        val focusedNode = mock<AccessibilityNodeInfo>()
        whenever(service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(focusedNode)

        val result = AccessibilityUtils.findActiveInputNode(service)

        assertEquals(focusedNode, result)
    }

    @Test
    fun `findActiveInputNode searches rootInActiveWindow when service findFocus returns null`() {
        val service = mock<AccessibilityService>()
        val root = mock<AccessibilityNodeInfo>()
        val inputNode = mock<AccessibilityNodeInfo>()

        whenever(service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(null)
        whenever(service.rootInActiveWindow).thenReturn(root)
        whenever(root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(null)

        whenever(root.childCount).thenReturn(1)
        whenever(root.getChild(0)).thenReturn(inputNode)
        whenever(inputNode.isFocused).thenReturn(true)
        whenever(inputNode.isEditable).thenReturn(true)

        val result = AccessibilityUtils.findActiveInputNode(service)

        assertEquals(inputNode, result)
    }

    @Test
    fun `findActiveInputNode returns null when service has no root window and no focus`() {
        val service = mock<AccessibilityService>()
        whenever(service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(null)
        whenever(service.rootInActiveWindow).thenReturn(null)

        val result = AccessibilityUtils.findActiveInputNode(service)

        assertNull(result)
    }
}
