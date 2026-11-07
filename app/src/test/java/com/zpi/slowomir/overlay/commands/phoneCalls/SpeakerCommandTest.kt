package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

class SpeakerCommandTest {

    private val mockContext = mock<Context>()
    private val mockExecutor = mock<PhoneCallExecutor>()
    private val mockExecutorProvider = mock<Provider<PhoneCallExecutor?>>()
    private val mockOverlayContext = mock<OverlayContext>()

    @Before
    fun setup() {
        whenever(mockExecutorProvider.get()).thenReturn(mockExecutor)
    }

    @Test
    fun `execute returns Failure when Accessibility Service is not enabled`() {
        whenever(mockExecutorProvider.get()).thenReturn(null)

        val command = SpeakerCommand(mockExecutorProvider, mockContext)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Accessibility Service is not enabled", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `execute delegates to PhoneCallExecutor speakerToggle and returns Success on true`() {
        whenever(mockExecutor.speakerToggle()).thenReturn(true)

        val command = SpeakerCommand(mockExecutorProvider, mockContext)

        val result = command.execute()

        assertTrue(result is CommandResult.Success)
        verify(mockExecutor).speakerToggle()
    }

    @Test
    fun `execute returns Failure when PhoneCallExecutor speakerToggle returns false`() {
        whenever(mockExecutor.speakerToggle()).thenReturn(false)

        val command = SpeakerCommand(mockExecutorProvider, mockContext)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Could not toggle speakerphone", (result as CommandResult.Failure).reason)
        verify(mockExecutor).speakerToggle()
    }

    @Test
    fun `canExecute returns true`() {
        val command = SpeakerCommand(mockExecutorProvider, mockContext)
        assertTrue(command.canExecute(mockOverlayContext))
    }
}
