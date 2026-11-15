package com.zpi.slowomir.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditEngineBoundaryTest {
    private val engine = TextEditEngine()
    private val operations: List<(EditorState) -> EditorState> = listOf(
        engine::cursorForward, engine::cursorBackward, engine::cursorWordNext,
        engine::cursorWordPrevious, engine::deleteChar, engine::deleteWord,
        { engine.insertText(it, "X") },
    )

    @Test
    fun `every operation rejects either invalid endpoint including integer extremes`() {
        for (invalid in listOf(-1, 4, Int.MIN_VALUE, Int.MAX_VALUE)) {
            for ((start, end) in listOf(invalid to 0, 0 to invalid, invalid to invalid)) {
                val state = EditorState("Ala", start, end)
                assertFalse(state.isSelectionValid)
                for (operation in operations) {
                    assertThrows(IllegalArgumentException::class.java) { operation(state) }
                }
            }
        }
    }

    @Test
    fun `all valid selections in short mixed texts preserve editor invariants`() {
        val texts = mutableListOf("")
        var level = listOf("")
        repeat(3) {
            level = level.flatMap { prefix -> listOf("a", "ą", "1", " ", "!", "\n").map { prefix + it } }
            texts.addAll(level)
        }
        for (text in texts) {
            for (start in 0..text.length) {
                for (end in 0..text.length) {
                    val state = EditorState(text, start, end)
                    val results = operations.map { it(state) }
                    for (result in results) {
                        assertTrue("Invalid output for $state", result.isSelectionValid)
                    }
                    results.take(4).forEach { assertEquals(text, it.textString) }
                    results.subList(4, 6).forEach { assertTrue(it.text.length <= text.length) }
                    val insertion = results.last()
                    assertEquals(text.length - kotlin.math.abs(end - start) + 1, insertion.text.length)
                    assertEquals(text, state.textString)
                    assertEquals(start, state.selectionStart)
                    assertEquals(end, state.selectionEnd)
                }
            }
        }
    }

    @Test
    fun `empty text and cursor boundaries stay unchanged`() {
        val empty = EditorState("", 0, 0)
        operations.dropLast(1).forEach { assertEquals(empty, it(empty)) }
        val start = EditorState("Ala", 0, 0)
        assertEquals(start, engine.cursorBackward(start))
        assertEquals(start, engine.cursorWordPrevious(start))
        assertEquals(start, engine.deleteChar(start))
        val end = EditorState("Ala", 3, 3)
        assertEquals(end, engine.cursorForward(end))
        assertEquals(end, engine.cursorWordNext(end))
    }

    @Test
    fun `reversed selection collapses or replaces the selected range`() {
        val state = EditorState("Ala ma kota", 6, 4)
        assertEquals(EditorState("Ala ma kota", 6, 6), engine.cursorForward(state))
        assertEquals(EditorState("Ala ma kota", 4, 4), engine.cursorBackward(state))
        assertEquals(EditorState("Ala  kota", 4, 4), engine.deleteChar(state))
        assertEquals(EditorState("Ala X kota", 5, 5), engine.insertText(state, "X"))
        assertEquals(EditorState("Ala  kota", 4, 4), engine.insertText(state, ""))
        assertEquals(EditorState("X", 1, 1), engine.insertText(EditorState("Ala", 0, 3), "X"))
    }

    @Test
    fun `character operations preserve surrogate pairs and combining characters`() {
        for (character in listOf("😀", "a\u0301", "ż")) {
            val text = character + "!"
            assertEquals(character.length, engine.cursorForward(EditorState(text, 0, 0)).selectionStart)
            assertEquals(0, engine.cursorBackward(EditorState(text, character.length, character.length)).selectionStart)
            assertEquals(EditorState("!", 0, 0), engine.deleteChar(EditorState(text, character.length, character.length)))
        }
    }

    @Test
    fun `deleteWord handles Polish letters digits and separator only text`() {
        for (position in 0.."żółć12".length) {
            assertEquals(EditorState("", 0, 0), engine.deleteWord(EditorState("żółć12", position, position)))
        }
        assertEquals(EditorState("", 0, 0), engine.deleteWord(EditorState(" \t\n!?", 5, 5)))
        assertEquals(EditorState(" kot!", 0, 0), engine.deleteWord(EditorState("Ala,\n kot!", 5, 5)))
        assertEquals(EditorState("", 0, 0), engine.deleteWord(EditorState("Ala", 3, 0)))
    }
}
