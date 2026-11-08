package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.phoneCalls.CallCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.WriteSMSCommand
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.MediatorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ContactSelectionManagerTest {

    private val mockMediator = mock<CommandMediator>()
    private val mockCommandResolver = mock<CommandResolver>()
    private val normalizer = PolishTextNormalizer()

    private val mediatorStateFlow = MutableStateFlow(MediatorState())

    private fun createManager(): ContactSelectionManager {
        whenever(mockMediator.mediatorState).thenReturn(mediatorStateFlow)
        return ContactSelectionManager(
            mediator = mockMediator,
            commandResolver = mockCommandResolver,
            normalizer = normalizer
        )
    }

    @Test
    fun `handleSpeech returns Selected with WriteSMSCommand when action is SMS_WRITE`() = runBlocking {
        val contactOptions = listOf(
            ContactOption(name = "Jan Kowalski (Komórka)", number = "+48111222333"),
            ContactOption(name = "Jan Kowalski (Dom)", number = "+48444555666")
        )
        mediatorStateFlow.value = MediatorState(
            contactSelection = contactOptions,
            contactSelectionAction = CommandIdentifier.SMS_WRITE
        )

        val mockSmsCommand = mock<WriteSMSCommand>()
        whenever(mockCommandResolver.createWriteSMS("+48111222333")).thenReturn(mockSmsCommand)

        val manager = createManager()
        val result = manager.handleSpeech("wybierz pierwszy")

        assertTrue(result is ContactSelectionSpeechResult.Selected)
        val selected = result as ContactSelectionSpeechResult.Selected
        assertEquals(CommandIdentifier.SMS_WRITE, selected.intent.identifier)
        assertEquals(CommandArgument.Text("+48111222333"), selected.intent.argument)
        assertEquals(mockSmsCommand, selected.command)

        verify(mockCommandResolver).createWriteSMS("+48111222333")
        verify(mockCommandResolver, never()).createCall(any())
        verify(mockMediator).handleEvent(OverlayEvent.SelectionCleared)
    }

    @Test
    fun `handleSpeech returns Selected with CallCommand when action is PHONE_CALL`() = runBlocking {
        val contactOptions = listOf(
            ContactOption(name = "Anna Nowak (Komórka)", number = "+48999888777"),
            ContactOption(name = "Anna Nowak (Praca)", number = "+48222333444")
        )
        mediatorStateFlow.value = MediatorState(
            contactSelection = contactOptions,
            contactSelectionAction = CommandIdentifier.PHONE_CALL
        )

        val mockCallCommand = mock<CallCommand>()
        whenever(mockCommandResolver.createCall("+48222333444")).thenReturn(mockCallCommand)

        val manager = createManager()
        val result = manager.handleSpeech("dwa")

        assertTrue(result is ContactSelectionSpeechResult.Selected)
        val selected = result as ContactSelectionSpeechResult.Selected
        assertEquals(CommandIdentifier.PHONE_CALL, selected.intent.identifier)
        assertEquals(CommandArgument.Text("+48222333444"), selected.intent.argument)
        assertEquals(mockCallCommand, selected.command)

        verify(mockCommandResolver).createCall("+48222333444")
        verify(mockCommandResolver, never()).createWriteSMS(any())
        verify(mockMediator).handleEvent(OverlayEvent.SelectionCleared)
    }

    @Test
    fun `handleSpeech returns Cancelled when speech matches cancel phrase`() = runBlocking {
        mediatorStateFlow.value = MediatorState(
            contactSelection = listOf(ContactOption(name = "Test", number = "123")),
            contactSelectionAction = CommandIdentifier.PHONE_CALL
        )

        val manager = createManager()
        val result = manager.handleSpeech("anuluj")

        assertTrue(result is ContactSelectionSpeechResult.Cancelled)
        assertEquals("anuluj", (result as ContactSelectionSpeechResult.Cancelled).rawText)
        verify(mockMediator).handleEvent(OverlayEvent.SelectionCleared)
    }

    @Test
    fun `selectContact creates WriteSMSCommand and notifies mediator on UI tap`() = runBlocking {
        val contactOptions = listOf(
            ContactOption(name = "Jan Kowalski (Komórka)", number = "+48111222333"),
            ContactOption(name = "Jan Kowalski (Dom)", number = "+48444555666")
        )
        mediatorStateFlow.value = MediatorState(
            contactSelection = contactOptions,
            contactSelectionAction = CommandIdentifier.SMS_WRITE
        )

        val mockSmsCommand = mock<WriteSMSCommand>()
        whenever(mockCommandResolver.createWriteSMS("+48444555666")).thenReturn(mockSmsCommand)

        val manager = createManager()
        manager.selectContact(2)

        verify(mockCommandResolver, timeout(2000)).createWriteSMS("+48444555666")
        verify(mockMediator, timeout(2000)).handleEvent(argThat {
            this is OverlayEvent.CommandInterpreted && this.command == mockSmsCommand
        })
    }

    @Test
    fun `cancelSelection clears selection in mediator`() = runBlocking {
        val manager = createManager()
        manager.cancelSelection()

        verify(mockMediator, timeout(2000)).handleEvent(OverlayEvent.SelectionCleared)
    }
}
