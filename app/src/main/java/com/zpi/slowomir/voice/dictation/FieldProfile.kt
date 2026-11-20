package com.zpi.slowomir.voice.dictation

import android.text.InputType
import android.view.inputmethod.EditorInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * How dictated text should be shaped for the focused text field, derived from the field's [EditorInfo]
 * the way a keyboard decides it.
 *
 * @property punctuation Whether commas, full stops and so on belong in this field.
 * @property uppercaseAll Whether the field asks for all capitals.
 * @property capsModes The capitalization modes the field asked for in its `inputType`.
 */
data class FieldProfile(
    val punctuation: Boolean = true,
    val uppercaseAll: Boolean = false,
    val capsModes: Int = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
) {
    companion object {
        private val NO_PUNCTUATION_VARIATIONS = setOf(
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_FILTER,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
        )

        private val PROSE_VARIATIONS = setOf(
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
        )

        private const val CAPS_FLAGS = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
            InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        private val SEARCH_HINT = Regex("szuka|wyszuk|search|find", RegexOption.IGNORE_CASE)

        val DEFAULT = FieldProfile()

        fun from(info: EditorInfo?): FieldProfile {
            val type = info?.inputType ?: return DEFAULT
            if (type == InputType.TYPE_NULL) return DEFAULT

            if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
                return FieldProfile(punctuation = false, capsModes = 0)
            }

            val variation = type and InputType.TYPE_MASK_VARIATION
            val flags = type and InputType.TYPE_MASK_FLAGS
            val uppercaseAll = flags and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0
            val capsModes = flags and CAPS_FLAGS

            val isSearch = info.imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_SEARCH ||
                info.hintText?.let { SEARCH_HINT.containsMatchIn(it) } == true
            if (isSearch || variation in NO_PUNCTUATION_VARIATIONS) {
                return FieldProfile(punctuation = false, uppercaseAll = uppercaseAll, capsModes = capsModes)
            }

            val prose = flags and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0 ||
                flags and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 ||
                variation in PROSE_VARIATIONS
            return FieldProfile(punctuation = prose, uppercaseAll = uppercaseAll, capsModes = capsModes)
        }
    }
}

/**
 * Holds the [FieldProfile] of the currently focused text field. Written by the accessibility service when a
 * field gains or loses focus, read when dictated text is prepared.
 */
@Singleton
class FieldProfileHolder @Inject constructor() {
    @Volatile
    var profile: FieldProfile = FieldProfile.DEFAULT
        private set

    fun update(info: EditorInfo?) {
        profile = FieldProfile.from(info)
    }

    fun reset() {
        profile = FieldProfile.DEFAULT
    }
}
