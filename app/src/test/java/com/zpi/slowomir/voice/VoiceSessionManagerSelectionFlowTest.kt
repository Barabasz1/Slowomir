package com.zpi.slowomir.voice

import android.content.Context
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.phoneCalls.CallCommand
import com.zpi.slowomir.overlay.commands.phoneCalls.WriteSMSCommand
import com.zpi.slowomir.utils.ContactLookupResult
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.utils.ContactUtils
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.MediatorState
import com.zpi.slowomir.voice.selection.SelectionManager
import com.zpi.slowomir.voice.stt.VoiceInput
import com.zpi.slowomir.voice.stt.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.after
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


class VoiceSessionManagerSelectionFlowTest {

    private val mediator = mock<CommandMediator>()
    private val intentParser = mock<CommandIntentParser>()
    private val commandResolver = mock<CommandResolver>()
    private val contactUtils = mock<ContactUtils>()
    private val applicationUtils = mock<com.zpi.slowomir.utils.ApplicationUtils>()

    private val selectionManager = SelectionManager(mock<Context>(), contactUtils, applicationUtils, PolishTextNormalizer())

    private val jan = listOf(
        ContactOption("Jan Kowalski (Komórka)", "+48111222333"),
        ContactOption("Jan Kowalski (Dom)", "+48444555666"),
    )

    private lateinit var sessionManager: VoiceSessionManager

    @Before
    fun setUp() {
        whenever(mediator.mediatorState).thenReturn(MutableStateFlow(MediatorState()))
        whenever(contactUtils.lookup(any(), eq("Jan"))).thenReturn(ContactLookupResult.Ambiguous(jan))

        sessionManager = VoiceSessionManager(mediator, intentParser, commandResolver, selectionManager)
        sessionManager.accept(VoiceInput.ServiceStarted)
        sessionManager.accept(VoiceInput.KeywordDetected)
        eventually("session is listening") { sessionManager.state.value is VoiceState.Listening }
    }


    @Test
    fun `spoken choice creates the SMS command for the chosen contact`() {
        val sms = mock<WriteSMSCommand>()
        stubIntent("napisz do Jan", CommandIdentifier.SMS_WRITE)
        stubCommand(CommandIdentifier.SMS_WRITE, "+48111222333", sms)

        sayAmbiguousUtterance("napisz do Jan")
        sessionManager.accept(VoiceInput.SpeechFinal("wybierz pierwszy"))

        verifyExecuted(sms)
        assertFalse(selectionManager.isActive)
    }

    @Test
    fun `spoken choice creates the call command for the chosen contact`() {
        val call = mock<CallCommand>()
        stubIntent("zadzwoń do Jan", CommandIdentifier.PHONE_CALL)
        stubCommand(CommandIdentifier.PHONE_CALL, "+48444555666", call)

        sayAmbiguousUtterance("zadzwoń do Jan")
        sessionManager.accept(VoiceInput.SpeechFinal("dwa"))

        verifyExecuted(call)
        assertFalse(selectionManager.isActive)
    }

    @Test
    fun `spoken choice is not parsed as a new command`() {
        stubIntent("zadzwoń do Jan", CommandIdentifier.PHONE_CALL)
        stubCommand(CommandIdentifier.PHONE_CALL, "+48444555666", mock<CallCommand>())

        sayAmbiguousUtterance("zadzwoń do Jan")
        sessionManager.accept(VoiceInput.SpeechFinal("dwa"))

        eventually("selection consumed") { !selectionManager.isActive }
        runBlocking { verify(intentParser, never()).parse("dwa") }
    }

    @Test
    fun `tapping an option creates the SMS command for that contact and executes it`() {
        val sms = mock<WriteSMSCommand>()
        stubIntent("napisz do Jan", CommandIdentifier.SMS_WRITE)
        stubCommand(CommandIdentifier.SMS_WRITE, "+48444555666", sms)

        sayAmbiguousUtterance("napisz do Jan")
        sessionManager.selectOption(2)

        verifyExecuted(sms)
        assertFalse(selectionManager.isActive)
    }

    @Test
    fun `tapping an option creates the call command for that contact and executes it`() {
        val call = mock<CallCommand>()
        stubIntent("zadzwoń do Jan", CommandIdentifier.PHONE_CALL)
        stubCommand(CommandIdentifier.PHONE_CALL, "+48111222333", call)

        sayAmbiguousUtterance("zadzwoń do Jan")
        sessionManager.selectOption(1)

        verifyExecuted(call)
    }

    @Test
    fun `tapping an option out of range keeps the selection and executes nothing`() {
        stubIntent("zadzwoń do Jan", CommandIdentifier.PHONE_CALL)

        sayAmbiguousUtterance("zadzwoń do Jan")
        sessionManager.selectOption(7)

        verify(commandResolver, after(300).never()).resolve(any())
        assertTrue(selectionManager.isActive)
    }

    @Test
    fun `spoken cancel clears the selection and executes nothing`() {
        stubIntent("zadzwoń do Jan", CommandIdentifier.PHONE_CALL)

        sayAmbiguousUtterance("zadzwoń do Jan")
        sessionManager.accept(VoiceInput.SpeechFinal("anuluj"))

        eventually("selection cancelled") { !selectionManager.isActive }
        verify(commandResolver, after(300).never()).resolve(any())
    }

    @Test
    fun `cancelSelection from the UI clears the selection and a later tap executes nothing`() {
        stubIntent("zadzwoń do Jan", CommandIdentifier.PHONE_CALL)

        sayAmbiguousUtterance("zadzwoń do Jan")
        sessionManager.cancelSelection()
        eventually("selection cancelled") { !selectionManager.isActive }

        sessionManager.selectOption(1)

        verify(commandResolver, after(300).never()).resolve(any())
    }

    private fun stubIntent(text: String, identifier: CommandIdentifier) {
        runBlocking {
            whenever(intentParser.parse(text))
                .thenReturn(VoiceIntent(identifier, CommandArgument.Text("Jan"), rawText = text))
        }
    }

    private fun stubCommand(identifier: CommandIdentifier, number: String, command: Command) {
        whenever(
            commandResolver.resolve(
                argThat { this.identifier == identifier && argument == CommandArgument.Text(number) }
            )
        ).thenReturn(command)
    }

    private fun sayAmbiguousUtterance(text: String) {
        sessionManager.accept(VoiceInput.SpeechFinal(text))
        eventually("selection pending and session listening again") {
            selectionManager.isActive && sessionManager.state.value is VoiceState.Listening
        }
    }

    private fun verifyExecuted(command: Command) {
        runBlocking {
            verify(mediator, timeout(2000)).handleEvent(argThat {
                this is OverlayEvent.CommandInterpreted && this.command === command
            })
        }
    }

    private fun eventually(what: String, timeoutMs: Long = 2000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("Timed out waiting for: $what")
            Thread.sleep(10)
        }
    }
}
