package com.zpi.slowomir.voice.stt

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.voice.VoiceIntent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class TextSessionStateMachineTest {
    private val command = mock<Command>()

    private fun intent(id: CommandIdentifier) = VoiceIntent(id, null, id.name)

    private fun reduce(state: VoiceState, event: SessionEvent) = VoiceStateMachine.reduce(state, event)

    private fun executing(id: CommandIdentifier, mode: TextMode? = null) =
        VoiceState.CommandExecuting(intent(id), command, mode)

    private val ok = InternalEvent.CommandFinished(CommandResult.Success())
    private val failed = InternalEvent.CommandFinished(CommandResult.Failure("no"))

    @Test
    fun `dictate starts dictation only when the executor accepted it`() {
        assertEquals(VoiceState.Dictating(), reduce(executing(CommandIdentifier.TEXT_ENTER), ok))
        assertEquals(VoiceState.Listening(), reduce(executing(CommandIdentifier.TEXT_ENTER), failed))
    }

    @Test
    fun `speech in dictation mode becomes text processing`() {
        assertEquals(
            VoiceState.Dictating("ala"),
            reduce(VoiceState.Dictating(), VoiceInput.SpeechPartial("ala")),
        )
        assertEquals(
            VoiceState.TextProcessing("ala ma kota", TextMode.DICTATE),
            reduce(VoiceState.Dictating(), VoiceInput.SpeechFinal("ala ma kota")),
        )
        assertEquals(VoiceState.Dictating(), reduce(VoiceState.Dictating(), VoiceInput.SpeechFinal("  ")))
    }

    @Test
    fun `typed text returns to the mode the speech came in`() {
        assertEquals(
            VoiceState.Dictating(),
            reduce(VoiceState.TextProcessing("ala", TextMode.DICTATE), InternalEvent.TextHandled),
        )
        assertEquals(
            VoiceState.Editing(),
            reduce(VoiceState.TextProcessing("ala", TextMode.EDIT), InternalEvent.TextHandled),
        )
    }

    @Test
    fun `edit command in dictation mode switches to edit mode`() {
        val processing = VoiceState.TextProcessing("edytuj", TextMode.DICTATE)
        val executingEdit = reduce(processing, InternalEvent.IntentParsed(intent(CommandIdentifier.TEXT_EDIT), command))
        assertEquals(executing(CommandIdentifier.TEXT_EDIT, TextMode.DICTATE), executingEdit)

        assertEquals(VoiceState.Editing(), reduce(executingEdit, ok))
        assertEquals(VoiceState.Dictating(), reduce(executingEdit, failed))
    }

    @Test
    fun `speech in edit mode is handled in edit mode`() {
        assertEquals(
            VoiceState.TextProcessing("usuń słowo", TextMode.EDIT),
            reduce(VoiceState.Editing(), VoiceInput.SpeechFinal("usuń słowo")),
        )
    }

    @Test
    fun `cursor and delete commands keep the edit session whatever their result`() {
        for (id in listOf(
            CommandIdentifier.TEXT_DELETE_WORD,
            CommandIdentifier.TEXT_DELETE_CHARACTER,
            CommandIdentifier.TEXT_CURSOR_FORWARD,
            CommandIdentifier.TEXT_CURSOR_PREVIOUS_WORD,
        )) {
            val processing = VoiceState.TextProcessing(id.name, TextMode.EDIT)
            val running = reduce(processing, InternalEvent.IntentParsed(intent(id), command))
            assertEquals(executing(id, TextMode.EDIT), running)
            assertEquals("$id ok", VoiceState.Editing(), reduce(running, ok))
            assertEquals("$id failed", VoiceState.Editing(), reduce(running, failed))
        }
    }

    @Test
    fun `dictate in edit mode goes back to dictation`() {
        val running = executing(CommandIdentifier.TEXT_ENTER, TextMode.EDIT)
        assertEquals(VoiceState.Dictating(), reduce(running, ok))
        assertEquals(VoiceState.Editing(), reduce(running, failed))
    }

    @Test
    fun `confirm and cancel end the session from both modes`() {
        for (mode in TextMode.entries) {
            for (id in listOf(CommandIdentifier.TEXT_CONFIRM, CommandIdentifier.TEXT_CANCEL)) {
                val running = executing(id, mode)
                assertEquals("$id/$mode ok", VoiceState.Listening(), reduce(running, ok))
                assertEquals("$id/$mode failed", VoiceState.Listening(), reduce(running, failed))
            }
        }
    }

    @Test
    fun `stop and keyword mode leave the session`() {
        val stop = InternalEvent.CommandFinished(CommandResult.Success(), returnToKws = true)
        assertEquals(VoiceState.KeywordSpotting, reduce(executing(CommandIdentifier.OVERLAY_STOP, TextMode.EDIT), stop))
        for (state in listOf(
            VoiceState.Dictating(),
            VoiceState.Editing(),
            VoiceState.TextProcessing("x", TextMode.EDIT),
        )) {
            assertEquals(VoiceState.KeywordSpotting, reduce(state, VoiceInput.EnableKwsMode))
        }
    }

    @Test
    fun `commands outside a text session behave as before`() {
        assertEquals(VoiceState.Listening(), reduce(executing(CommandIdentifier.TEXT_DELETE_WORD), ok))
        assertEquals(VoiceState.Listening(), reduce(executing(CommandIdentifier.TEXT_EDIT), failed))
    }
}
