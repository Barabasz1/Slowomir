package com.zpi.slowomir.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextEditEngineRegressionTest {
    private val engine = TextEditEngine()

    @Test
    fun `test cursorWordNext after deletion`() {
        val state1 = EditorState("Ala ma kota", 6, 6) // "Ala ma| kota"
        val state2 = engine.deleteWord(state1)
        assertEquals("Ala  kota", state2.text.toString())
        assertEquals(4, state2.selectionStart) // "Ala | kota"

        val state3 = engine.cursorWordNext(state2)
        // From the separator, move to the start of the next word.
        assertEquals(5, state3.selectionStart)
    }

    @Test
    fun `test cursor stays in place and deletes full word when deleting from middle`() {
        val state1 = EditorState("Ala kota", 6, 6) // "Ala ko|ta"
        val state2 = engine.deleteWord(state1)
        
        assertEquals("Ala ", state2.text.toString())
        assertEquals(4, state2.selectionStart)
    }

    @Test
    fun `test delete word when exactly at the end of word`() {
        val state1 = EditorState("Ala kota", 8, 8) // "Ala kota|"
        val state2 = engine.deleteWord(state1)
        
        assertEquals("Ala ", state2.text.toString())
        assertEquals(4, state2.selectionStart)
    }

    @Test
    fun `test delete word when after space but before next word`() {
        val state1 = EditorState("Ala kota ", 9, 9) // "Ala kota |"
        val state2 = engine.deleteWord(state1)
        
        assertEquals("Ala ", state2.text.toString())
        assertEquals(4, state2.selectionStart)
    }

    @Test
    fun `test delete word when cursor is moved one char forward from start`() {
        val state1 = EditorState("Ala ma kota", 0, 0)
        val state2 = engine.cursorForward(state1) // "A|la ma kota"
        assertEquals(1, state2.selectionStart)

        val state3 = engine.deleteWord(state2)
        assertEquals(" ma kota", state3.text.toString())
        assertEquals(0, state3.selectionStart)
    }

    @Test
    fun `test cursorWordPrevious then cursorForward then deleteWord at the start of text`() {
        var state = EditorState("Ala ma kota", 4, 4) // "Ala |ma kota"
        
        state = engine.cursorWordPrevious(state)
        assertEquals(0, state.selectionStart) // "|Ala ma kota"
        
        state = engine.cursorForward(state)
        assertEquals(1, state.selectionStart) // "A|la ma kota"
        
        state = engine.deleteWord(state)
        
        assertEquals(" ma kota", state.text.toString())
        assertEquals(0, state.selectionStart)
    }

    @Test
    fun `test cursorWordPrevious then cursorForward twice then deleteWord at the start of text`() {
        var state = EditorState("Ala ma kota", 4, 4) // "Ala |ma kota"
        
        state = engine.cursorWordPrevious(state)
        assertEquals(0, state.selectionStart) // "|Ala ma kota"
        
        state = engine.cursorForward(state)
        state = engine.cursorForward(state)
        assertEquals(2, state.selectionStart) // "Al|a ma kota"
        
        state = engine.deleteWord(state)
        
        assertEquals(" ma kota", state.text.toString())
        assertEquals(0, state.selectionStart)
    }
}
