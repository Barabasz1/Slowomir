package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
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

class WriteSMSCommandTest {

    private val mockContext = mock<Context>()
    private val mockExecutor = mock<PhoneCallExecutor>()
    private val mockExecutorProvider = mock<Provider<PhoneCallExecutor?>>()

    @Before
    fun setup() {
        whenever(mockExecutorProvider.get()).thenReturn(mockExecutor)
    }

    @Test
    fun `execute returns Failure when recipient is empty`() {
        val command = WriteSMSCommand(mockExecutorProvider, mockContext, recipient = "")

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("No contact was given.", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `execute delegates to PhoneCallExecutor when executor is present`() {
        val number = "987654321"
        whenever(mockExecutor.writeSMS(number)).thenReturn(true)

        val command = WriteSMSCommand(mockExecutorProvider, mockContext, recipient = number)

        val result = command.execute()

        assertTrue(result is CommandResult.Success)
        verify(mockExecutorProvider).get()
        verify(mockExecutor).writeSMS(number)
    }

    @Test
    fun `execute returns Failure when PhoneCallExecutor writeSMS returns false`() {
        val number = "987654321"
        whenever(mockExecutor.writeSMS(number)).thenReturn(false)

        val command = WriteSMSCommand(mockExecutorProvider, mockContext, recipient = number)

        val result = command.execute()

        assertTrue(result is CommandResult.Failure)
        assertEquals("Write SMS failed", (result as CommandResult.Failure).reason)
    }
}
