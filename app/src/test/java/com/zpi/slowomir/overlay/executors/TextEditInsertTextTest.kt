package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.text.TextUtils
import android.view.inputmethod.SurroundingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TextEditInsertTextTest {
    private val service = mock<AccessibilityService>() // no active window: no leading space is added
    private val touch = mock<TouchExecutor>()

    private val capsOn = TextUtils.CAP_MODE_SENTENCES

    private fun connectionWithCaps(caps: Int): InputMethod.AccessibilityInputConnection {
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        whenever(connection.getCursorCapsMode(any())).thenReturn(caps)
        return connection
    }

    @Test
    fun `insertText capitalizes the first letter when the editor asks for a capital`() {
        val connection = connectionWithCaps(capsOn)
        assertTrue(TextEditExecutor(service, touch) { connection }.insertText("ala ma kota."))
        verify(connection).commitText(eq("Ala ma kota."), eq(1), isNull())
    }

    @Test
    fun `insertText lowercases the first letter in the middle of a sentence`() {
        val connection = connectionWithCaps(0)
        assertTrue(TextEditExecutor(service, touch) { connection }.insertText("Ala ma kota."))
        verify(connection).commitText(eq("ala ma kota."), eq(1), isNull())
    }

    @Test
    fun `first letter case keeps acronyms and leaves the rest of the text alone`() {
        assertEquals("NATO ma Kraków", TextEditExecutor.applyFirstLetterCase("NATO ma Kraków", capitalize = false))
        assertEquals("ala ma Kraków", TextEditExecutor.applyFirstLetterCase("Ala ma Kraków", capitalize = false))
        assertEquals("Ala ma Kraków", TextEditExecutor.applyFirstLetterCase("ala ma Kraków", capitalize = true))
    }

    private fun connectionWithContext(text: String, selection: Int, caps: Int = 0): InputMethod.AccessibilityInputConnection {
        val context = mock<SurroundingText>()
        whenever(context.text).thenReturn(text)
        whenever(context.selectionStart).thenReturn(selection)
        whenever(context.selectionEnd).thenReturn(selection)
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        whenever(connection.getSurroundingText(eq(1), eq(0), eq(0))).thenReturn(context)
        whenever(connection.getCursorCapsMode(any())).thenReturn(caps)
        return connection
    }

    @Test
    fun `insertText adds a space after a non-whitespace character`() {
        val connection = connectionWithContext("a", selection = 1)
        assertTrue(TextEditExecutor(service, touch) { connection }.insertText("kota"))
        verify(connection).commitText(eq(" "), eq(1), isNull())
        verify(connection).commitText(eq("kota"), eq(1), isNull())
    }

    @Test
    fun `insertText adds no space after whitespace or at the start of the field`() {
        val afterSpace = connectionWithContext(" ", selection = 1)
        TextEditExecutor(service, touch) { afterSpace }.insertText("kota")
        verify(afterSpace).commitText(eq("kota"), eq(1), isNull())

        val atStart = connectionWithContext("", selection = 0)
        TextEditExecutor(service, touch) { atStart }.insertText("kota")
        verify(atStart).commitText(eq("kota"), eq(1), isNull())
    }

    @Test
    fun `the caps mode is read after the space so a sentence start is recognised`() {
        val connection = connectionWithContext(".", selection = 1, caps = capsOn)
        TextEditExecutor(service, touch) { connection }.insertText("kota")

        val order = inOrder(connection)
        order.verify(connection).commitText(eq(" "), eq(1), isNull())
        order.verify(connection).getCursorCapsMode(any())
        order.verify(connection).commitText(eq("Kota"), eq(1), isNull())
    }

    @Test
    fun `insertText fails without a connected text field`() {
        assertFalse(TextEditExecutor(service, touch) { null }.insertText("Ala"))
    }

    @Test
    fun `insertText ignores blank text`() {
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        assertFalse(TextEditExecutor(service, touch) { connection }.insertText("  "))
        verify(connection, never()).commitText(any(), any(), any())
    }

    @Test
    fun `enter fails without a text field and confirm fails when not dictating`() {
        val executor = TextEditExecutor(service, touch) { null }
        assertFalse(executor.enter())
        assertFalse(executor.isDictating)
        assertFalse(executor.confirm())
    }

    @Test
    fun `confirm ends dictation and keeps the text`() {
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        val executor = TextEditExecutor(service, touch) { connection }

        assertTrue(executor.enter())
        assertTrue(executor.isDictating)
        assertTrue(executor.confirm())
        assertFalse(executor.isDictating)
        verify(connection, never()).deleteSurroundingText(any(), any())
    }

    @Test
    fun `cancel deletes exactly the text typed since enter`() {
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        val executor = TextEditExecutor(service, touch) { connection }

        assertTrue(executor.enter())
        assertTrue(executor.insertText("Ala ma"))
        assertTrue(executor.insertText("kota"))
        assertTrue(executor.cancel())

        verify(connection).deleteSurroundingText(eq("Ala ma".length + "kota".length), eq(0))
        assertFalse(executor.isDictating)
        assertFalse(executor.cancel())
    }
}
