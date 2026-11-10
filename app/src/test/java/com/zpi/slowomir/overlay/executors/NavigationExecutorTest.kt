package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.content.res.Resources
import android.util.DisplayMetrics
import com.zpi.slowomir.utils.ScreenPoint
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class NavigationExecutorTest {

    private lateinit var service: AccessibilityService
    private lateinit var resources: Resources
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var mockTouchExecutor: TouchExecutor
    private lateinit var executor: NavigationExecutor

    @Before
    fun setUp() {
        service = mock()
        resources = mock()
        displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }

        whenever(service.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(displayMetrics)
        
        whenever(service.performGlobalAction(any())).thenReturn(true)

        mockTouchExecutor = mock()
        whenever(mockTouchExecutor.swipeUp(any<ScreenPoint>(), anyOrNull(), any(), any())).thenReturn(true)
        whenever(mockTouchExecutor.swipeDown(any<ScreenPoint>(), anyOrNull(), any(), any())).thenReturn(true)
        whenever(mockTouchExecutor.swipeLeft(any<ScreenPoint>(), anyOrNull(), any(), any())).thenReturn(true)
        whenever(mockTouchExecutor.swipeRight(any<ScreenPoint>(), anyOrNull(), any(), any())).thenReturn(true)
        whenever(mockTouchExecutor.pinchIn(any<ScreenPoint>(), any(), any(), any())).thenReturn(true)
        whenever(mockTouchExecutor.pinchOut(any<ScreenPoint>(), any(), any(), any())).thenReturn(true)

        executor = NavigationExecutor(service, mockTouchExecutor)
    }

    @Test
    fun `navigateBack should perform GLOBAL_ACTION_BACK`() {
        val result = executor.navigateBack()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    @Test
    fun `navigateHome should perform GLOBAL_ACTION_HOME`() {
        val result = executor.navigateHome()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    @Test
    fun `navigateRecent should perform GLOBAL_ACTION_RECENTS`() {
        val result = executor.navigateRecent()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    @Test
    fun `notifications should perform GLOBAL_ACTION_NOTIFICATIONS`() {
        val result = executor.notifications()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    @Test
    fun `quickSettings should perform GLOBAL_ACTION_QUICK_SETTINGS`() {
        val result = executor.quickSettings()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    @Test
    fun `lockScreen should perform GLOBAL_ACTION_LOCK_SCREEN`() {
        val result = executor.lockScreen()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    @Test
    fun `screenshot should perform GLOBAL_ACTION_TAKE_SCREENSHOT`() {
        val result = executor.screenshot()

        assertTrue(result)
        verify(service).performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    @Test
    fun `swipeUp should dispatch gesture`() {
        val result = executor.swipeUp(0.5f, 0.8f)

        assertTrue(result)
        verify(mockTouchExecutor).swipeUp(any<ScreenPoint>(), anyOrNull(), any(), any())
    }

    @Test
    fun `swipeDown should dispatch gesture`() {
        val result = executor.swipeDown(0.5f, 0.2f)

        assertTrue(result)
        verify(mockTouchExecutor).swipeDown(any<ScreenPoint>(), anyOrNull(), any(), any())
    }

    @Test
    fun `swipeLeft should dispatch gesture`() {
        val result = executor.swipeLeft(0.8f, 0.5f)

        assertTrue(result)
        verify(mockTouchExecutor).swipeLeft(any<ScreenPoint>(), anyOrNull(), any(), any())
    }

    @Test
    fun `swipeRight should dispatch gesture`() {
        val result = executor.swipeRight(0.2f, 0.5f)

        assertTrue(result)
        verify(mockTouchExecutor).swipeRight(any<ScreenPoint>(), anyOrNull(), any(), any())
    }

    @Test
    fun `zoomIn should dispatch gesture`() {
        val result = executor.zoomIn(0.5f, 0.5f)

        assertTrue(result)
        verify(mockTouchExecutor).pinchOut(any<ScreenPoint>(), any(), any(), any())
    }

    @Test
    fun `zoomOut should dispatch gesture`() {
        val result = executor.zoomOut(0.5f, 0.5f)

        assertTrue(result)
        verify(mockTouchExecutor).pinchIn(any<ScreenPoint>(), any(), any(), any())
    }
}
