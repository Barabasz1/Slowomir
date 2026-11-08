package com.zpi.slowomir.overlay.commands.phoneCalls

import android.content.Context
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.PhoneCallExecutor
import com.zpi.slowomir.utils.ContactLookupResult
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.utils.ContactUtils
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

class WriteSMSCommandTest {

    private val mockContext = mock<Context>()
    private val mockExecutor = mock<PhoneCallExecutor>()
    private val mockExecutorProvider = mock<Provider<PhoneCallExecutor?>>()

    private val mockContactUtils = mock<ContactUtils>()

    @Before
    fun setup() {
        whenever(mockExecutorProvider.get()).thenReturn(mockExecutor)
    }

    @Test
    fun `execute returns Failure when recipient is empty`() {
        // Arrange
        whenever(mockContactUtils.lookup(any(), eq(""))).thenReturn(ContactLookupResult.EmptyQuery)

        val command = WriteSMSCommand(mockExecutorProvider, mockContactUtils, mockContext, recipient = "")

        // Act
        val result = command.execute()

        // Assert
        assertTrue(result is CommandResult.Failure)
        assertEquals("No contact was given.", (result as CommandResult.Failure).reason)
    }

    @Test
    fun `execute returns NeedsSelection when contact lookup is ambiguous`() {
        // Arrange
        val options = listOf(
            ContactOption("Kasia Dom", "111222333"),
            ContactOption("Kasia Praca", "444555666")
        )
        whenever(mockContactUtils.lookup(any(), eq("Kasia"))).thenReturn(ContactLookupResult.Ambiguous(options))

        val command = WriteSMSCommand(mockExecutorProvider, mockContactUtils, mockContext, recipient = "Kasia")

        // Act
        val result = command.execute()

        // Assert
        assertTrue(result is CommandResult.NeedsSelection)
        assertEquals(options, (result as CommandResult.NeedsSelection).options)
    }

    @Test
    fun `execute delegates to PhoneCallExecutor when contact is Found and executor is present`() {
        // Arrange
        val number = "987654321"
        whenever(mockContactUtils.lookup(any(), eq("Mama"))).thenReturn(ContactLookupResult.Found("Mama", number))

        whenever(mockExecutor.writeSMS(number)).thenReturn(true)

        val command = WriteSMSCommand(mockExecutorProvider, mockContactUtils, mockContext, recipient = "Mama")

        // Act
        val result = command.execute()

        // Assert
        assertTrue(result is CommandResult.Success)
        verify(mockExecutorProvider).get()
        verify(mockExecutor).writeSMS(number)
    }

    @Test
    fun `execute returns Failure when contact is NotFound`() {
        // Arrange
        whenever(mockContactUtils.lookup(any(), eq("Janek"))).thenReturn(ContactLookupResult.NotFound)

        val command = WriteSMSCommand(mockExecutorProvider, mockContactUtils, mockContext, recipient = "Janek")

        // Act
        val result = command.execute()

        // Assert
        assertTrue(result is CommandResult.Failure)
        assertEquals("Could not find a contact: Janek", (result as CommandResult.Failure).reason)
    }
}
