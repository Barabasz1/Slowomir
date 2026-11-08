package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.res.Resources
import android.graphics.Path
import android.util.DisplayMetrics
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.*

class NavigationExecutorTest {

    private lateinit var service: AccessibilityService
    private lateinit var resources: Resources
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var executor: NavigationExecutor

    private lateinit var builderConstruction: MockedConstruction<GestureDescription.Builder>
    private lateinit var strokeConstruction: MockedConstruction<GestureDescription.StrokeDescription>
    private lateinit var pathConstruction: MockedConstruction<Path>

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
        whenever(service.dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(true)

        val mockGesture = mock<GestureDescription>()
        builderConstruction = mockConstruction(GestureDescription.Builder::class.java) { mock, _ ->
            whenever(mock.addStroke(anyOrNull())).thenReturn(mock)
            whenever(mock.build()).thenReturn(mockGesture)
        }

        strokeConstruction = mockConstruction(GestureDescription.StrokeDescription::class.java) { mock, _ ->
            whenever(mock.continueStroke(anyOrNull(), any(), any(), any())).thenReturn(mock)
        }

        pathConstruction = mockConstruction(Path::class.java) { mock, _ ->
            // do nothing
        }

        executor = NavigationExecutor(service)
    }

    @After
    fun tearDown() {
        builderConstruction.close()
        strokeConstruction.close()
        pathConstruction.close()
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
        val result = executor.swipeUp(500f, 1000f)

        assertTrue(result)
        verify(service, atLeastOnce()).dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `swipeDown should dispatch gesture`() {
        val result = executor.swipeDown(500f, 1000f)

        assertTrue(result)
        verify(service, atLeastOnce()).dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `swipeLeft should dispatch gesture`() {
        val result = executor.swipeLeft(500f, 1000f)

        assertTrue(result)
        verify(service, atLeastOnce()).dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `swipeRight should dispatch gesture`() {
        val result = executor.swipeRight(500f, 1000f)

        assertTrue(result)
        verify(service, atLeastOnce()).dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `zoomIn should dispatch gesture`() {
        val result = executor.zoomIn(500f, 1000f)

        assertTrue(result)
        verify(service, atLeastOnce()).dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `zoomOut should dispatch gesture`() {
        val result = executor.zoomOut(500f, 1000f)

        assertTrue(result)
        verify(service, atLeastOnce()).dispatchGesture(anyOrNull(), anyOrNull(), anyOrNull())
    }
}
