package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
import android.telecom.TelecomManager
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

class CallCommandTest {

    private val mockContext = mock<Context>()
    private val mockExecutor = mock<PhoneCallExecutor>()
    private val mockExecutorProvider = mock<Provider<PhoneCallExecutor?>>()
    private val mockTelecomManager = mock<TelecomManager>()

    @Before
    fun setup() {
        whenever(mockExecutorProvider.get()).thenReturn(mockExecutor)
        whenever(mockContext.getSystemService(Context.TELECOM_SERVICE)).thenReturn(mockTelecomManager)
    }

    @Test
    fun `execute returns Failure when recipient is empty`() {
        val command = CallCommand(mockExecutorProvider, mockContext, recipient = "")

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("No contact was given.", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `execute returns Failure when recipient is null`() {
        val command = CallCommand(mockExecutorProvider, mockContext, recipient = null)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
    }

    @Test
    fun `execute delegates to PhoneCallExecutor when executor is present`() {
        val number = "987654321"
        whenever(mockExecutor.call(number)).thenReturn(true)

        val command = CallCommand(mockExecutorProvider, mockContext, recipient = number)

        val result = command.execute()

        assertTrue(result is CommandResult.Success)
        verify(mockExecutorProvider).get()
        verify(mockExecutor).call(number)
    }

    @Test
    fun `execute returns Failure when PhoneCallExecutor call returns false`() {
        val number = "987654321"
        whenever(mockExecutor.call(number)).thenReturn(false)

        val command = CallCommand(mockExecutorProvider, mockContext, recipient = number)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Phone call failed", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `execute falls back to TelecomManager placeCall when PhoneCallExecutor is null`() {
        val mockUri = mock<android.net.Uri>()
        org.mockito.Mockito.mockStatic(android.net.Uri::class.java).use { uriMock ->
            uriMock.`when`<android.net.Uri> { android.net.Uri.parse(any()) }.thenReturn(mockUri)

            whenever(mockExecutorProvider.get()).thenReturn(null)

            val command = CallCommand(mockExecutorProvider, mockContext, recipient = "123456789")

            val result = command.execute()

            assertTrue(result is CommandResult.Success)
            verify(mockTelecomManager).placeCall(eq(mockUri), eq(null))
        }
    }

    @Test
    fun `execute returns Failure when fallback placeCall throws exception`() {
        whenever(mockExecutorProvider.get()).thenReturn(null)
        whenever(mockTelecomManager.placeCall(any(), eq(null))).thenThrow(SecurityException("Permission denied"))

        val command = CallCommand(mockExecutorProvider, mockContext, recipient = "123456789")

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Phone call failed", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `canExecute returns true`() {
        val command = CallCommand(mockExecutorProvider, mockContext, recipient = "123456789")
        assertTrue(command.canExecute(mock()))
    }
}
