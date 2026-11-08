package com.zpi.slowomir.overlay.commands.phoneCalls

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

class PickUpCommandTest {

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

        val command = PickUpCommand(mockExecutorProvider)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Accessibility Service is not enabled", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `execute delegates to PhoneCallExecutor pickUp and returns Success on true`() {
        whenever(mockExecutor.pickUp()).thenReturn(true)

        val command = PickUpCommand(mockExecutorProvider)

        val result = command.execute()

        assertTrue(result is CommandResult.Success)
        verify(mockExecutor).pickUp()
    }

    @Test
    fun `execute returns Failure when PhoneCallExecutor pickUp returns false`() {
        whenever(mockExecutor.pickUp()).thenReturn(false)

        val command = PickUpCommand(mockExecutorProvider)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Phone call pick up failed", (result as CommandResult.Failure).reason)
        verify(mockExecutor).pickUp()
    }

    @Test
    fun `canExecute returns true`() {
        val command = PickUpCommand(mockExecutorProvider)
        assertTrue(command.canExecute(mockOverlayContext))
    }
}
