package com.zpi.slowomir.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TextDeleteWordRegressionTest {
    private val engine = TextEditEngine()

    @Test
    fun `deleteWord removes first word at every word position`() {
        for (position in listOf(0, 1, 2, 4, 7)) {
            val result = engine.deleteWord(EditorState("fwafbhw  jhfawhj", position, position))
            assertEquals("  jhfawhj", result.textString)
            assertEquals(0, result.selectionStart)
            assertEquals(0, result.selectionEnd)
        }
    }

    @Test
    fun `delete middle word then first word after one or two cursor movements`() {
        for (steps in 1..2) {
            var state = engine.deleteWord(EditorState("fwafbhw bhfawh jhfawhj", 10, 10))
            assertEquals("fwafbhw  jhfawhj", state.textString)
            state = state.copy(selectionStart = 0, selectionEnd = 0)
            repeat(steps) { state = engine.cursorForward(state) }
            assertEquals(steps, state.selectionStart)
            state = engine.deleteWord(state)
            assertEquals("  jhfawhj", state.textString)
            assertEquals(0, state.selectionStart)
            assertEquals(0, state.selectionEnd)
        }
    }

    @Test
    fun `deleteWord respects reversed selection`() {
        val result = engine.deleteWord(EditorState("Ala ma kota", 6, 4))
        assertEquals("Ala  kota", result.textString)
        assertEquals(4, result.selectionStart)
        assertEquals(4, result.selectionEnd)
    }

    @Test
    fun `empty text and leading separators are no ops`() {
        for (state in listOf(EditorState("", 0, 0), EditorState(" Ala", 0, 0))) {
            assertEquals(state, engine.deleteWord(state))
        }
    }

    @Test
    fun `deleteWord preserves punctuation after a word and removes preceding separators`() {
        assertEquals(", kot", engine.deleteWord(EditorState("Ala, kot", 1, 1)).textString)
        assertEquals(" kot", engine.deleteWord(EditorState("Ala, kot", 4, 4)).textString)
        assertEquals("Ala, ", engine.deleteWord(EditorState("Ala, kot", 8, 8)).textString)
    }

    @Test
    fun `invalid selections are rejected before computing edits`() {
        for ((start, end) in listOf(-1 to -1, -1 to 2, 1 to 9)) {
            val state = EditorState("Ala", start, end)
            assertFalse(state.isSelectionValid)
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                engine.deleteWord(state)
            }
        }
    }
}
