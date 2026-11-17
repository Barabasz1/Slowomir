package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.view.inputmethod.SurroundingText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TextEditInsertTextTest {
    private val service = mock<AccessibilityService>() // no active window: no leading space is added
    private val touch = mock<TouchExecutor>()

    @Test
    fun `insertText commits the text through the input connection`() {
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        val executor = TextEditExecutor(service, touch) { connection }

        assertTrue(executor.insertText("Ala ma kota."))
        verify(connection).commitText(eq("Ala ma kota."), eq(1), isNull())
    }

    private fun connectionWithContext(text: String, selection: Int): InputMethod.AccessibilityInputConnection {
        val context = mock<SurroundingText>()
        whenever(context.text).thenReturn(text)
        whenever(context.selectionStart).thenReturn(selection)
        whenever(context.selectionEnd).thenReturn(selection)
        val connection = mock<InputMethod.AccessibilityInputConnection>()
        whenever(connection.getSurroundingText(eq(1), eq(0), eq(0))).thenReturn(context)
        return connection
    }

    @Test
    fun `insertText adds a space after a non-whitespace character`() {
        val connection = connectionWithContext("a", selection = 1)
        assertTrue(TextEditExecutor(service, touch) { connection }.insertText("kota"))
        verify(connection).commitText(eq(" kota"), eq(1), isNull())
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
