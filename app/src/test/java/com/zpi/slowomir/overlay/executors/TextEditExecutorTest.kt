package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.SHOW_MODE_AUTO
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TextEditExecutorTest {

    private lateinit var mockService: AccessibilityService
    private lateinit var mockSoftKeyboardController: AccessibilityService.SoftKeyboardController
    private lateinit var mockTouchExecutor: TouchExecutor
    private lateinit var textEditExecutor: TextEditExecutor

    @Before
    fun setUp() {
        mockService = mock()
        mockSoftKeyboardController = mock()
        mockTouchExecutor = mock()

        whenever(mockService.softKeyboardController).thenReturn(mockSoftKeyboardController)
        whenever(mockSoftKeyboardController.setShowMode(SHOW_MODE_AUTO)).thenReturn(true)
        whenever(mockService.windows).thenReturn(emptyList())

        textEditExecutor = TextEditExecutor(mockService, mockTouchExecutor)
    }

    @Test
    fun `keyboardShow returns true immediately when keyboard is already visible`() {
        val imeWindow = mock<AccessibilityWindowInfo>()
        whenever(imeWindow.type).thenReturn(AccessibilityWindowInfo.TYPE_INPUT_METHOD)
        whenever(mockService.windows).thenReturn(listOf(imeWindow))

        val result = textEditExecutor.keyboardShow()

        assertTrue(result)
        verify(mockTouchExecutor, never()).tapNode(any())
    }

    @Test
    fun `keyboardShow delegates to touchExecutor tapNode with focused input node`() {
        val mockNode = mock<AccessibilityNodeInfo>()
        whenever(mockService.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(mockNode)
        whenever(mockTouchExecutor.tapNode(mockNode)).thenReturn(true)

        val result = textEditExecutor.keyboardShow()

        assertTrue(result)
        verify(mockSoftKeyboardController).showMode = SHOW_MODE_AUTO
        verify(mockTouchExecutor).tapNode(mockNode)
    }

    @Test
    fun `keyboardShow returns false when no node is found`() {
        whenever(mockService.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(null)
        whenever(mockService.rootInActiveWindow).thenReturn(null)

        val result = textEditExecutor.keyboardShow()

        assertFalse(result)
    }

    @Test
    fun `keyboardHide skips back action when keyboard is already hidden`() {
        whenever(mockService.windows).thenReturn(emptyList())

        val result = textEditExecutor.keyboardHide()

        assertTrue(result)
        verify(mockSoftKeyboardController).showMode = SHOW_MODE_AUTO
        verify(mockService, never()).performGlobalAction(GLOBAL_ACTION_BACK)
    }

    @Test
    fun `keyboardHide executes GLOBAL_ACTION_BACK when keyboard is visible`() {
        val imeWindow = mock<AccessibilityWindowInfo>()
        whenever(imeWindow.type).thenReturn(AccessibilityWindowInfo.TYPE_INPUT_METHOD)
        whenever(mockService.windows).thenReturn(listOf(imeWindow))
        whenever(mockService.performGlobalAction(GLOBAL_ACTION_BACK)).thenReturn(true)

        val result = textEditExecutor.keyboardHide()

        assertTrue(result)
        verify(mockSoftKeyboardController).showMode = SHOW_MODE_AUTO
        verify(mockService).performGlobalAction(GLOBAL_ACTION_BACK)
    }

    @Test
    fun `keyboardToggle shows keyboard when hidden and hides when visible`() {
        // 1. When hidden -> calls keyboardShow
        whenever(mockService.windows).thenReturn(emptyList())
        val mockNode = mock<AccessibilityNodeInfo>()
        whenever(mockService.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)).thenReturn(mockNode)
        whenever(mockTouchExecutor.tapNode(mockNode)).thenReturn(true)

        val showResult = textEditExecutor.keyboardToggle()
        assertTrue(showResult)
        verify(mockTouchExecutor).tapNode(mockNode)

        // 2. When visible -> calls keyboardHide
        val imeWindow = mock<AccessibilityWindowInfo>()
        whenever(imeWindow.type).thenReturn(AccessibilityWindowInfo.TYPE_INPUT_METHOD)
        whenever(mockService.windows).thenReturn(listOf(imeWindow))
        whenever(mockService.performGlobalAction(GLOBAL_ACTION_BACK)).thenReturn(true)

        val hideResult = textEditExecutor.keyboardToggle()
        assertTrue(hideResult)
        verify(mockService).performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
