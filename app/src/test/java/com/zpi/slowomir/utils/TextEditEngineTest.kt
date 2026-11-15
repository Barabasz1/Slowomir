package com.zpi.slowomir.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextEditEngineTest {

    private val engine = TextEditEngine()

    @Test
    fun `cursorForward should move one character right`() {
        val state = EditorState("Ala ma kota", 1, 1) // "A|la ma kota"
        val result = engine.cursorForward(state)
        assertEquals(2, result.selectionStart)
    }

    @Test
    fun `cursorForward should collapse selection to the right`() {
        val state = EditorState("Ala ma kota", 1, 3) // "A[la] ma kota"
        val result = engine.cursorForward(state)
        assertEquals(3, result.selectionStart)
        assertEquals(3, result.selectionEnd)
    }

    @Test
    fun `cursorBackward should move one character left`() {
        val state = EditorState("Ala ma kota", 2, 2) // "Al|a ma kota"
        val result = engine.cursorBackward(state)
        assertEquals(1, result.selectionStart)
    }

    @Test
    fun `cursorWordNext should jump to start of next word`() {
        val state = EditorState("Ala ma kota", 1, 1) // "A|la ma kota"
        val result = engine.cursorWordNext(state)
        assertEquals(4, result.selectionStart) // "Ala |ma kota"
    }

    @Test
    fun `cursorWordNext should stay if no next word`() {
        val state = EditorState("Ala ma kota", 8, 8) // "Ala ma k|ota"
        val result = engine.cursorWordNext(state)
        assertEquals(11, result.selectionStart) // "Ala ma kota|"
    }

    @Test
    fun `cursorWordPrevious should jump to start of word if inside word`() {
        val state = EditorState("Ala ma kota", 9, 9) // "Ala ma ko|ta"
        val result = engine.cursorWordPrevious(state)
        assertEquals(7, result.selectionStart) // "Ala ma |kota"
    }

    @Test
    fun `cursorWordPrevious should jump to previous word if at start of word`() {
        val state = EditorState("Ala ma kota", 4, 4) // "Ala |ma kota"
        val result = engine.cursorWordPrevious(state)
        assertEquals(0, result.selectionStart) // "|Ala ma kota"
    }

    @Test
    fun `deleteChar should remove preceding character`() {
        val state = EditorState("Cześć", 4, 4) // "Cześ|ć"
        val result = engine.deleteChar(state)
        assertEquals("Czeć", result.text.toString())
        assertEquals(3, result.selectionStart)
    }

    @Test
    fun `deleteChar should delete selection if present`() {
        val state = EditorState("Cześć", 1, 4) // "C[ześ]ć"
        val result = engine.deleteChar(state)
        assertEquals("Cć", result.text.toString())
        assertEquals(1, result.selectionStart)
    }

    @Test
    fun `deleteWord should remove word before cursor`() {
        val state = EditorState("Cześć, będę jutro", 17, 17) // "Cześć, będę jutro|"
        val result = engine.deleteWord(state)
        assertEquals("Cześć, będę ", result.text.toString())
        assertEquals(12, result.selectionStart)
    }

    @Test
    fun `deleteWord should remove spaces and previous word`() {
        val state = EditorState("Ala ma   ", 9, 9) // "Ala ma   |"
        val result = engine.deleteWord(state)
        assertEquals("Ala ", result.text.toString())
        assertEquals(4, result.selectionStart)
    }

    @Test
    fun `deleteWord should remove both sides of word if inside word`() {
        val state = EditorState("Ala kota", 6, 6) // "Ala ko|ta"
        val result = engine.deleteWord(state)
        assertEquals("Ala ", result.text.toString())
        assertEquals(4, result.selectionStart)
    }

    @Test
    fun `deleteWord should remove the whole word when cursor is exactly at the beginning of a word`() {
        val state = EditorState("Ala ma kota", 7, 7) // "Ala ma |kota"
        val result = engine.deleteWord(state)
        assertEquals("Ala ma ", result.text.toString())
        assertEquals(7, result.selectionStart)
    }

    @Test
    fun `insertText should place text at cursor and advance it`() {
        val state = EditorState("Ala ma kota", 7, 7) // "Ala ma |kota"
        val result = engine.insertText(state, "rudego ")
        assertEquals("Ala ma rudego kota", result.text.toString())
        assertEquals(14, result.selectionStart)
    }
}
