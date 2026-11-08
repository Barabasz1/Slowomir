package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.phoneCalls.WriteSMSCommand
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.MediatorState
import com.zpi.slowomir.voice.stt.VoiceInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class VoiceSessionManagerContactSelectionTest {

    private val mockMediator = mock<CommandMediator>()
    private val mockIntentParser = mock<CommandIntentParser>()
    private val mockCommandResolver = mock<CommandResolver>()
    private val mockContactSelectionManager = mock<ContactSelectionManager>()

    private val mediatorStateFlow = MutableStateFlow(MediatorState())

    private fun createSessionManager(): VoiceSessionManager {
        whenever(mockMediator.mediatorState).thenReturn(mediatorStateFlow)
        return VoiceSessionManager(
            mediator = mockMediator,
            intentParser = mockIntentParser,
            commandResolver = mockCommandResolver,
            contactSelectionManager = mockContactSelectionManager,
        )
    }

    @Test
    fun `selectContact delegates to ContactSelectionManager`() {
        val sessionManager = createSessionManager()
        sessionManager.selectContact(2)

        verify(mockContactSelectionManager).selectContact(2)
    }

    @Test
    fun `cancelSelection delegates to ContactSelectionManager`() {
        val sessionManager = createSessionManager()
        sessionManager.cancelSelection()

        verify(mockContactSelectionManager).cancelSelection()
    }

    @Test
    fun `speech during pending contact selection delegates to ContactSelectionManager`() {
        mediatorStateFlow.value = MediatorState(
            contactSelection = mock(),
            contactSelectionAction = CommandIdentifier.SMS_WRITE
        )
        val mockCommand = mock<WriteSMSCommand>()
        runBlocking {
            whenever(mockContactSelectionManager.handleSpeech(any())).thenReturn(
                ContactSelectionSpeechResult.Selected(
                    intent = mock(),
                    command = mockCommand
                )
            )
        }

        val sessionManager = createSessionManager()
        sessionManager.accept(VoiceInput.SpeechFinal("wybierz pierwszy"))

        runBlocking {
            verify(mockContactSelectionManager, timeout(2000)).handleSpeech("wybierz pierwszy")
        }
    }
}
