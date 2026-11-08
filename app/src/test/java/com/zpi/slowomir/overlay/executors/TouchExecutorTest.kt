package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TouchExecutorTest {

    private lateinit var mockService: AccessibilityService
    private lateinit var touchExecutor: TouchExecutor

    @Before
    fun setUp() {
        mockService = mock()
        touchExecutor = TouchExecutor(mockService)
    }

    @Test
    fun `click performs ACTION_CLICK when node supports it`() {
        val mockNode = mock<AccessibilityNodeInfo>()
        val mockAction = AccessibilityNodeInfo.AccessibilityAction(
            AccessibilityNodeInfo.ACTION_CLICK,
            "click",
        )
        whenever(mockNode.actionList).thenReturn(listOf(mockAction))
        whenever(mockNode.isClickable).thenReturn(true)
        whenever(mockNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)).thenReturn(true)

        val result = touchExecutor.tapNode(mockNode)

        assertTrue(result)
        verify(mockNode).performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    @Test
    fun `click traverses to clickable parent if target node does not support click`() {
        val childNode = mock<AccessibilityNodeInfo>()
        val parentNode = mock<AccessibilityNodeInfo>()

        whenever(childNode.actionList).thenReturn(emptyList())
        whenever(childNode.isClickable).thenReturn(false)
        whenever(childNode.parent).thenReturn(parentNode)

        whenever(parentNode.actionList).thenReturn(
            listOf(
                AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, "click"),
            ),
        )
        whenever(parentNode.isClickable).thenReturn(true)
        whenever(parentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)).thenReturn(true)

        val result = touchExecutor.tapNode(childNode)

        assertTrue(result)
        verify(parentNode).performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    @Test
    fun `click returns false when semantic click fails and bounds are empty`() {
        val mockNode = mock<AccessibilityNodeInfo>()
        whenever(mockNode.actionList).thenReturn(emptyList())
        whenever(mockNode.isClickable).thenReturn(false)
        whenever(mockNode.parent).thenReturn(null)

        doAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.setEmpty()
            null
        }.whenever(mockNode).getBoundsInScreen(any())

        val result = touchExecutor.tapNode(mockNode)

        assertFalse(result)
    }

    @Test
    fun `click returns false when semantic click fails and coordinates are non-positive`() {
        val mockNode = mock<AccessibilityNodeInfo>()
        whenever(mockNode.actionList).thenReturn(emptyList())
        whenever(mockNode.isClickable).thenReturn(false)
        whenever(mockNode.parent).thenReturn(null)

        doAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.setEmpty()
            null
        }.whenever(mockNode).getBoundsInScreen(any())

        val result = touchExecutor.tapNode(mockNode)

        assertFalse(result)
    }
}
