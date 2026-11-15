package com.zpi.slowomir.utils

import java.text.BreakIterator

/**
 * Immutable state of a text editor.
 *
 * @property text The current text content.
 * @property selectionStart The start index of the selected text.
 * @property selectionEnd The end index of the selected text.
 */
data class EditorState(
    val text: CharSequence,
    val selectionStart: Int,
    val selectionEnd: Int
) {
    val isSelectionValid: Boolean
        get() = selectionStart in 0..text.length && selectionEnd in 0..text.length

    val hasSelection: Boolean
        get() = selectionStart != selectionEnd

    val safeSelectionStart: Int
        get() = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)

    val safeSelectionEnd: Int
        get() = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        
    val textString: String
        get() = text.toString()
}

/**
 * Pure Kotlin engine for text editing operations (cursor movement and deletion).
 */
class TextEditEngine {
    /**
     * Moves the cursor forward in the text.
     *
     * @param state The current editor state.
     * @return The new editor state with the cursor moved forward.
     */
    fun cursorForward(state: EditorState): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        val safeEnd = state.safeSelectionEnd

        if (state.hasSelection) {
            return state.copy(selectionStart = safeEnd, selectionEnd = safeEnd)
        }

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(state.textString)
        val next = iterator.following(safeEnd)

        return if (next != BreakIterator.DONE) {
            state.copy(selectionStart = next, selectionEnd = next)
        } else {
            state
        }
    }

    /**
     * Moves the cursor backward in the text.
     *
     * @param state The current editor state.
     * @return The new editor state with the cursor moved backward.
     */
    fun cursorBackward(state: EditorState): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        val safeStart = state.safeSelectionStart

        if (state.hasSelection) {
            return state.copy(selectionStart = safeStart, selectionEnd = safeStart)
        }

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(state.textString)
        val prev = iterator.preceding(safeStart)

        return if (prev != BreakIterator.DONE) {
            state.copy(selectionStart = prev, selectionEnd = prev)
        } else {
            state
        }
    }

    /**
     * Moves the cursor to the next word in the text.
     *
     * @param state The current editor state.
     * @return The new editor state with the cursor moved to the next word.
     */
    fun cursorWordNext(state: EditorState): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        val safeEnd = state.safeSelectionEnd
        val textStr = state.textString
        if (safeEnd >= textStr.length) return state

        var current = safeEnd
        
        while (current < textStr.length && textStr[current].isLetterOrDigit()) {
            current++
        }
        
        while (current < textStr.length && !textStr[current].isLetterOrDigit()) {
            current++
        }

        return state.copy(selectionStart = current, selectionEnd = current)
    }

    /**
     * Moves the cursor to the previous word in the text.
     *
     * @param state The current editor state.
     * @return The new editor state with the cursor moved to the previous word.
     */
    fun cursorWordPrevious(state: EditorState): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        val safeStart = state.safeSelectionStart
        val textStr = state.textString
        if (safeStart <= 0) return state

        var current = safeStart
        
        if (textStr[current - 1].isLetterOrDigit() && (current == textStr.length || !textStr[current].isLetterOrDigit())) {
            current--
        } else {
            while (current > 0 && !textStr[current - 1].isLetterOrDigit()) {
                current--
            }
        }
        
        while (current > 0 && textStr[current - 1].isLetterOrDigit()) {
            current--
        }

        return state.copy(selectionStart = current, selectionEnd = current)
    }

    /**
     * Concatenates two character sequences.
     *
     * @param part1 The first character sequence.
     * @param part2 The second character sequence.
     * @return The concatenated character sequence.
     */
    private fun safeConcat(part1: CharSequence, part2: CharSequence): CharSequence {
        return part1.toString() + part2.toString()
    }

    /**
     * Deletes the character under the cursor.
     *
     * @param state The current editor state.
     * @return The new editor state with the character deleted.
     */
    fun deleteChar(state: EditorState): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        if (state.hasSelection) {
            val start = state.safeSelectionStart
            val end = state.safeSelectionEnd
            val newText = safeConcat(state.text.subSequence(0, start), state.text.subSequence(end, state.text.length))
            return EditorState(newText, start, start)
        }

        val start = state.safeSelectionStart
        if (start == 0) return state

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(state.textString)
        val prev = iterator.preceding(start)

        if (prev != BreakIterator.DONE) {
            val newText = safeConcat(state.text.subSequence(0, prev), state.text.subSequence(start, state.text.length))
            return EditorState(newText, prev, prev)
        }

        return state
    }

    /**
     * Deletes the word under the cursor.
     *
     * @param state The current editor state.
     * @return The new editor state with the word deleted.
     */
    fun deleteWord(state: EditorState): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        if (state.hasSelection) {
            val start = state.safeSelectionStart
            val end = state.safeSelectionEnd
            val newText = safeConcat(state.text.subSequence(0, start), state.text.subSequence(end, state.text.length))
            return EditorState(newText, start, start)
        }

        val safeStart = state.safeSelectionStart
        val textStr = state.textString
        
        if (textStr.isEmpty()) return state
        
        var deleteStart = safeStart
        var deleteEnd = safeStart
        
        val isInsideWord = safeStart > 0 && textStr[safeStart - 1].isLetterOrDigit() && safeStart < textStr.length && textStr[safeStart].isLetterOrDigit()
        val isAtWordStart = safeStart < textStr.length && textStr[safeStart].isLetterOrDigit() && (safeStart == 0 || !textStr[safeStart - 1].isLetterOrDigit())
        val isAtWordEnd = safeStart > 0 && textStr[safeStart - 1].isLetterOrDigit() && (safeStart == textStr.length || !textStr[safeStart].isLetterOrDigit())
        
        if (isInsideWord) {
            while (deleteStart > 0 && textStr[deleteStart - 1].isLetterOrDigit()) {
                deleteStart--
            }
            while (deleteEnd < textStr.length && textStr[deleteEnd].isLetterOrDigit()) {
                deleteEnd++
            }
        } else if (isAtWordStart) {
            while (deleteEnd < textStr.length && textStr[deleteEnd].isLetterOrDigit()) {
                deleteEnd++
            }
        } else if (isAtWordEnd) {
            while (deleteStart > 0 && textStr[deleteStart - 1].isLetterOrDigit()) {
                deleteStart--
            }
        } else {
            while (deleteStart > 0 && !textStr[deleteStart - 1].isLetterOrDigit()) {
                deleteStart--
            }
            while (deleteStart > 0 && textStr[deleteStart - 1].isLetterOrDigit()) {
                deleteStart--
            }
        }

        if (deleteStart < deleteEnd) {
            val newText = safeConcat(state.text.subSequence(0, deleteStart), state.text.subSequence(deleteEnd, state.text.length))
            return EditorState(newText, deleteStart, deleteStart)
        }

        return state
    }

    /**
     * Inserts text at the cursor position.
     *
     * @param state The current editor state.
     * @param newText The text to insert.
     * @return The new editor state with the text inserted.
     */
    fun insertText(state: EditorState, newText: String): EditorState {
        require(state.isSelectionValid) { "Selection is outside the text bounds" }
        val start = state.safeSelectionStart
        val end = state.safeSelectionEnd
        val resultText = safeConcat(safeConcat(state.text.subSequence(0, start), newText), state.text.subSequence(end, state.text.length))
        val newCursor = start + newText.length
        return EditorState(resultText, newCursor, newCursor)
    }
}
