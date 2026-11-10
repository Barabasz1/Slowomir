package com.zpi.slowomir.voice.selection

import android.content.Context
import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.utils.ContactLookupResult
import com.zpi.slowomir.utils.ContactOption
import com.zpi.slowomir.utils.ContactUtils
import com.zpi.slowomir.voice.PolishTextNormalizer
import com.zpi.slowomir.voice.VoiceIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SelectionManagerTest {

    private val context = mock<Context>()
    private val contactUtils = mock<ContactUtils>()
    private val applicationUtils = mock<com.zpi.slowomir.utils.ApplicationUtils>()
    private val manager = SelectionManager(context, contactUtils, applicationUtils, PolishTextNormalizer())

    private val options = listOf(
        ContactOption("Kasia Dom", "111222333"),
        ContactOption("Kasia Praca", "444555666"),
    )

    private fun intent(id: CommandIdentifier, query: String? = "Kasia") =
        VoiceIntent(id, query?.let { CommandArgument.Text(it) }, rawText = "raw")

    private fun beginAmbiguous(id: CommandIdentifier): VoiceIntent {
        val original = intent(id)
        whenever(contactUtils.lookup(any(), eq("Kasia"))).thenReturn(ContactLookupResult.Ambiguous(options))
        val result = runBlocking { manager.begin(original) }
        assertEquals(SelectionResult.Pending, result)
        return original
    }

    @Test
    fun `begin resolves a single match to its number`() = runBlocking {
        whenever(contactUtils.lookup(any(), eq("Mama"))).thenReturn(ContactLookupResult.Found("Mama", "600100001"))

        val result = manager.begin(intent(CommandIdentifier.PHONE_CALL, "Mama"))

        assertEquals(
            SelectionResult.Resolved(intent(CommandIdentifier.PHONE_CALL, "600100001")),
            result,
        )
        assertFalse(manager.isActive)
    }

    @Test
    fun `begin publishes a contact selection when lookup is ambiguous`() {
        val original = beginAmbiguous(CommandIdentifier.SMS_WRITE)

        assertEquals(Selection.Contact(original, options), manager.selection.value)
        assertTrue(manager.isActive)
    }

    @Test
    fun `begin fails for not found, permission denied and empty query`() = runBlocking {
        whenever(contactUtils.lookup(any(), eq("Janek"))).thenReturn(ContactLookupResult.NotFound)
        whenever(contactUtils.lookup(any(), eq("Tata"))).thenReturn(ContactLookupResult.PermissionDenied)
        whenever(contactUtils.lookup(any(), eq(""))).thenReturn(ContactLookupResult.EmptyQuery)

        assertEquals(
            SelectionResult.Failed("Could not find a contact: Janek"),
            manager.begin(intent(CommandIdentifier.PHONE_CALL, "Janek")),
        )
        assertEquals(
            SelectionResult.Failed("No access to reading contacts"),
            manager.begin(intent(CommandIdentifier.PHONE_CALL, "Tata")),
        )
        assertEquals(
            SelectionResult.Failed("No contact was given."),
            manager.begin(intent(CommandIdentifier.PHONE_CALL, null)),
        )
        assertFalse(manager.isActive)
    }

    @Test
    fun `begin fails for intents without a selection`() = runBlocking {
        val result = manager.begin(intent(CommandIdentifier.VOLUME_UP))

        assertTrue(result is SelectionResult.Failed)
    }

    @Test
    fun `handleSpeech chooses the option and keeps the original action`() {
        beginAmbiguous(CommandIdentifier.SMS_WRITE)

        val result = manager.handleSpeech("wybierz pierwszy")

        assertEquals(
            SelectionSpeechResult.Chosen(intent(CommandIdentifier.SMS_WRITE, "111222333")),
            result,
        )
        assertFalse(manager.isActive)
    }

    @Test
    fun `handleSpeech chooses the second option for a call`() {
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        val result = manager.handleSpeech("dwa")

        assertEquals(
            SelectionSpeechResult.Chosen(intent(CommandIdentifier.PHONE_CALL, "444555666")),
            result,
        )
    }

    @Test
    fun `handleSpeech cancels on a cancel phrase`() {
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        val result = manager.handleSpeech("anuluj")

        assertEquals(SelectionSpeechResult.Cancelled("anuluj"), result)
        assertFalse(manager.isActive)
    }

    @Test
    fun `handleSpeech ignores unrelated speech and keeps the selection`() {
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        val result = manager.handleSpeech("jaka jest pogoda")

        assertEquals(SelectionSpeechResult.Ignored("jaka jest pogoda"), result)
        assertTrue(manager.isActive)
    }

    @Test
    fun `choose with an out of range index keeps the selection`() {
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        assertNull(manager.choose(5))
        assertTrue(manager.isActive)
    }

    @Test
    fun `choose only succeeds once`() {
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        assertEquals(intent(CommandIdentifier.PHONE_CALL, "111222333"), manager.choose(1))
        assertNull(manager.choose(1))
    }

    @Test
    fun `selection is dropped after the timeout`() {
        manager.timeoutMs = 50
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        Thread.sleep(300)

        assertFalse(manager.isActive)
    }

    @Test
    fun `timeout of a finished selection never drops a newer one`() {
        manager.timeoutMs = 200
        beginAmbiguous(CommandIdentifier.PHONE_CALL)
        manager.choose(1)
        manager.timeoutMs = 10_000
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        Thread.sleep(400)

        assertTrue(manager.isActive)
    }

    @Test
    fun `cancel clears the selection`() {
        beginAmbiguous(CommandIdentifier.PHONE_CALL)

        assertTrue(manager.cancel())
        assertFalse(manager.isActive)
        assertFalse(manager.cancel())
    }
}
