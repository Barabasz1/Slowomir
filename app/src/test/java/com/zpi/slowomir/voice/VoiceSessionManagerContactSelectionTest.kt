package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.phoneCalls.CallCommand
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.mediator.MediatorState
import com.zpi.slowomir.voice.selection.SelectionResult
import com.zpi.slowomir.voice.selection.SelectionManager
import com.zpi.slowomir.voice.selection.SelectionSpeechResult
import com.zpi.slowomir.voice.stt.VoiceInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class VoiceSessionManagerContactSelectionTest {

    private val mockMediator = mock<CommandMediator>()
    private val mockIntentParser = mock<CommandIntentParser>()
    private val mockCommandResolver = mock<CommandResolver>()
    private val mockSelectionManager = mock<SelectionManager>()

    private fun createSessionManager(): VoiceSessionManager {
        whenever(mockMediator.mediatorState).thenReturn(MutableStateFlow(MediatorState()))
        whenever(mockSelectionManager.selection).thenReturn(MutableStateFlow(null))
        return VoiceSessionManager(
            mediator = mockMediator,
            intentParser = mockIntentParser,
            commandResolver = mockCommandResolver,
            selectionManager = mockSelectionManager,
        )
    }

    @Test
    fun `selectOption chooses the option in SelectionManager`() {
        val sessionManager = createSessionManager()
        sessionManager.selectOption(2)

        verify(mockSelectionManager, timeout(2000)).choose(2)
    }

    @Test
    fun `cancelSelection cancels the selection in SelectionManager`() {
        val sessionManager = createSessionManager()
        sessionManager.cancelSelection()

        verify(mockSelectionManager, timeout(2000)).cancel()
    }

    @Test
    fun `speech during pending selection is handled by SelectionManager and not parsed`() {
        whenever(mockSelectionManager.isActive).thenReturn(true)
        val chosen = VoiceIntent(CommandIdentifier.SMS_WRITE, CommandArgument.Text("123"))
        whenever(mockSelectionManager.handleSpeech(any())).thenReturn(SelectionSpeechResult.Chosen(chosen))
        whenever(mockCommandResolver.resolve(chosen)).thenReturn(mock<CallCommand>())

        val sessionManager = createSessionManager()
        sessionManager.accept(VoiceInput.SpeechFinal("wybierz pierwszy"))

        verify(mockSelectionManager, timeout(2000)).handleSpeech("wybierz pierwszy")
        verify(mockCommandResolver, timeout(2000)).resolve(chosen)
        verify(mockIntentParser, never()).parse(any())
    }

    @Test
    fun `call intent is sent through SelectionManager begin before command resolution`() {
        val intent = VoiceIntent(CommandIdentifier.PHONE_CALL, CommandArgument.Text("Kasia"))
        val resolved = intent.copy(argument = CommandArgument.Text("111222333"))
        whenever(mockSelectionManager.isActive).thenReturn(false)
        runBlocking {
            whenever(mockIntentParser.parse("zadzwoń do Kasi")).thenReturn(intent)
            whenever(mockSelectionManager.begin(intent)).thenReturn(SelectionResult.Resolved(resolved))
        }
        whenever(mockCommandResolver.resolve(resolved)).thenReturn(mock<CallCommand>())

        val sessionManager = createSessionManager()
        sessionManager.accept(VoiceInput.SpeechFinal("zadzwoń do Kasi"))

        verify(mockCommandResolver, timeout(2000)).resolve(resolved)
    }

    @Test
    fun `non selectable intent skips SelectionManager begin`() {
        val intent = VoiceIntent(CommandIdentifier.VOLUME_UP)
        whenever(mockSelectionManager.isActive).thenReturn(false)
        runBlocking { whenever(mockIntentParser.parse("głośniej")).thenReturn(intent) }
        whenever(mockCommandResolver.resolve(intent)).thenReturn(mock<CallCommand>())

        val sessionManager = createSessionManager()
        sessionManager.accept(VoiceInput.SpeechFinal("głośniej"))

        verify(mockCommandResolver, timeout(2000)).resolve(intent)
        runBlocking { verify(mockSelectionManager, never()).begin(any()) }
    }
}
