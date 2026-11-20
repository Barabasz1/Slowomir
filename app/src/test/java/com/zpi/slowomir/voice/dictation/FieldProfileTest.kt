package com.zpi.slowomir.voice.dictation

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldProfileTest {

    private fun field(inputType: Int, imeOptions: Int = 0, hint: CharSequence? = null) =
        FieldProfile.from(EditorInfo().apply {
            this.inputType = inputType
            this.imeOptions = imeOptions
            this.hintText = hint
        })

    private val text = InputType.TYPE_CLASS_TEXT

    @Test
    fun `fields with no information get the default profile`() {
        assertEquals(FieldProfile.DEFAULT, FieldProfile.from(null))
        assertEquals(FieldProfile.DEFAULT, field(InputType.TYPE_NULL))
    }

    @Test
    fun `notes and messages get punctuation`() {
        assertTrue(field(text or InputType.TYPE_TEXT_FLAG_MULTI_LINE).punctuation)
        assertTrue(field(text or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES).punctuation)
        assertTrue(field(text or InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE).punctuation)
        assertTrue(field(text or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE).punctuation)
    }

    @Test
    fun `plain single line text without capital flags gets none`() {
        assertFalse(field(text).punctuation)
    }

    @Test
    fun `search fields get no punctuation even when they ask for a capital`() {
        val sentenceCaps = text or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        assertFalse(field(sentenceCaps, imeOptions = EditorInfo.IME_ACTION_SEARCH).punctuation)
        assertFalse(field(sentenceCaps, hint = "Szukaj w notatkach").punctuation)
        assertFalse(field(sentenceCaps, hint = "Search").punctuation)
    }

    @Test
    fun `address email password name and filter fields get no punctuation`() {
        val sentenceCaps = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        for (variation in listOf(
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_FILTER,
        )) {
            assertFalse("variation $variation", field(text or variation or sentenceCaps).punctuation)
        }
    }

    @Test
    fun `numeric phone and date fields get no punctuation`() {
        assertFalse(field(InputType.TYPE_CLASS_NUMBER).punctuation)
        assertFalse(field(InputType.TYPE_CLASS_PHONE).punctuation)
        assertFalse(field(InputType.TYPE_CLASS_DATETIME).punctuation)
    }

    @Test
    fun `all capitals fields are recognised`() {
        assertTrue(field(text or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS).uppercaseAll)
        assertFalse(field(text or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES).uppercaseAll)
    }

    @Test
    fun `only the capitalization modes the field asked for are reported`() {
        assertEquals(0, field(text).capsModes) // e.g. a search box
        assertEquals(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, field(text or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES).capsModes)
        assertEquals(
            InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            field(text or InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES).capsModes,
        )
        assertEquals(0, field(InputType.TYPE_CLASS_NUMBER).capsModes)
    }

    @Test
    fun `holder follows focus and resets when the field is gone`() {
        val holder = FieldProfileHolder()
        assertEquals(FieldProfile.DEFAULT, holder.profile)

        holder.update(EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER })
        assertFalse(holder.profile.punctuation)

        holder.reset()
        assertEquals(FieldProfile.DEFAULT, holder.profile)
    }
}
