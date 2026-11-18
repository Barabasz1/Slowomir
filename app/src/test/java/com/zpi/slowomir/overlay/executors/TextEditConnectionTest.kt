package com.zpi.slowomir.overlay.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
import android.view.inputmethod.SurroundingText
import com.zpi.slowomir.utils.EditorState
import com.zpi.slowomir.utils.TextEditEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TextEditConnectionTest {

    private class FakeField(
        var text: String,
        var start: Int,
        var end: Int = start,
        private val reportOffset: Boolean = true,
    ) {
        val connection: InputMethod.AccessibilityInputConnection =
            mock<InputMethod.AccessibilityInputConnection>().also { c ->
                whenever(c.getSurroundingText(any(), any(), any())).thenAnswer { call ->
                    val before = call.getArgument<Int>(0)
                    val after = call.getArgument<Int>(1)
                    val lo = maxOf(0, minOf(start, end) - before)
                    val hi = minOf(text.length, maxOf(start, end) + after)
                    val context = mock<SurroundingText>()
                    whenever(context.text).thenReturn(text.substring(lo, hi))
                    whenever(context.selectionStart).thenReturn(start - lo)
                    whenever(context.selectionEnd).thenReturn(end - lo)
                    whenever(context.offset).thenReturn(if (reportOffset) lo else -1)
                    context
                }
                doAnswer { call ->
                    start = call.getArgument(0)
                    end = call.getArgument(1)
                    null
                }.whenever(c).setSelection(any(), any())
                doAnswer { call ->
                    val typed = call.getArgument<CharSequence>(0).toString()
                    val lo = minOf(start, end)
                    val hi = maxOf(start, end)
                    text = text.substring(0, lo) + typed + text.substring(hi)
                    start = lo + typed.length
                    end = start
                    null
                }.whenever(c).commitText(any(), any(), anyOrNull())
                doAnswer { call ->
                    val before = call.getArgument<Int>(0)
                    val after = call.getArgument<Int>(1)
                    val lo = maxOf(0, minOf(start, end) - before)
                    val hi = minOf(text.length, maxOf(start, end) + after)
                    text = text.substring(0, lo) + text.substring(hi)
                    start = lo
                    end = lo
                    null
                }.whenever(c).deleteSurroundingText(any(), any())
            }
    }

    private val service = mock<AccessibilityService>()
    private val touch = mock<TouchExecutor>()
    private val engine = TextEditEngine()

    private fun executor(field: FakeField?) = TextEditExecutor(service, touch) { field?.connection }

    private fun expected(field: FakeField, op: (EditorState) -> EditorState): EditorState =
        op(EditorState(field.text, field.start, field.end))

    private fun assertFieldMatches(field: FakeField, expected: EditorState) {
        assertEquals(expected.textString, field.text)
        assertEquals(expected.safeSelectionStart, minOf(field.start, field.end))
        assertEquals(expected.safeSelectionEnd, maxOf(field.start, field.end))
    }

    @Test
    fun `deleteChar deletes the character before the cursor`() {
        val field = FakeField("Ala ma", start = 6)
        val want = expected(field) { engine.deleteChar(it) }

        assertTrue(executor(field).deleteChar())
        assertFieldMatches(field, want)
        assertEquals("Ala m", field.text)
    }

    @Test
    fun `deleteWord deletes in the middle of the text and keeps the rest`() {
        val field = FakeField("Ala ma kota", start = 6)
        val want = expected(field) { engine.deleteWord(it) }

        assertTrue(executor(field).deleteWord())
        assertFieldMatches(field, want)
    }

    @Test
    fun `deleting a selection removes exactly the selected text`() {
        val field = FakeField("Ala ma kota", start = 4, end = 6)
        val want = expected(field) { engine.deleteChar(it) }

        assertTrue(executor(field).deleteChar())
        assertFieldMatches(field, want)
        assertEquals("Ala  kota", field.text)
    }

    @Test
    fun `an editor that reports no text offset still works when the whole text start was returned`() {
        val field = FakeField("Ala ma kota", start = 6, reportOffset = false)
        val want = expected(field) { engine.deleteWord(it) }

        assertTrue(executor(field).deleteWord())
        assertFieldMatches(field, want)
    }

    @Test
    fun `cursor commands only move the selection`() {
        val field = FakeField("Ala ma kota", start = 3)
        val want = expected(field) { engine.cursorWordNext(it) }

        assertTrue(executor(field).cursorWordNext())
        assertFieldMatches(field, want)
        assertEquals("Ala ma kota", field.text)
        verify(field.connection, never()).commitText(any(), any(), anyOrNull())
        verify(field.connection, never()).deleteSurroundingText(any(), any())
    }

    @Test
    fun `an operation that changes nothing fails like at the text boundary`() {
        val field = FakeField("Ala", start = 0)
        assertFalse(executor(field).cursorBackward())
        assertFalse(executor(field).deleteChar())
        assertEquals("Ala", field.text)
    }

    @Test
    fun `a field that ignores the write is reported as failed`() {
        val field = FakeField("Ala ma", start = 6)
        doAnswer { null }.whenever(field.connection).deleteSurroundingText(any(), any())

        assertFalse(executor(field).deleteChar())
        assertEquals("Ala ma", field.text)
    }

    @Test
    fun `without a usable connection the node path is used and fails cleanly when there is no node`() {
        assertFalse(executor(null).deleteChar())

        val noContext = mock<InputMethod.AccessibilityInputConnection>()
        assertFalse(TextEditExecutor(service, touch) { noContext }.deleteChar())
        verify(noContext, never()).deleteSurroundingText(any(), any())
        verify(noContext, never()).commitText(any(), any(), anyOrNull())
    }

    @Test
    fun `starting a sentence is recognised from the text before the cursor`() {
        assertTrue(TextEditExecutor.startsSentence(""))
        assertTrue(TextEditExecutor.startsSentence("Ala ma kota. "))
        assertTrue(TextEditExecutor.startsSentence("Naprawdę?"))
        assertFalse(TextEditExecutor.startsSentence("Ala ma"))
        assertFalse(TextEditExecutor.startsSentence("Ala, "))
    }

    @Test
    fun `dictation falls back without a text field and does not type`() {
        val executor = executor(null)
        assertFalse(executor.enter())
        assertFalse(executor.insertText("Ala ma kota"))
    }
}
