package com.zpi.slowomir.overlay.voice

import javax.inject.Inject

/**
 * Normalizes text input in Polish language, replacing word-based numbers with their corresponding
 * numeric values, and stripping punctuation marks.
 */
class PolishTextNormalizer @Inject constructor() {

    // Map can yet be extended or replaced with a better solution to handle more values if necessary.
    private val wordToNumberMap = mapOf(
        "dwadzieścia" to "20",
        "dziewiętnaście" to "19",
        "osiemnaście" to "18",
        "siedemnaście" to "17",
        "szesnaście" to "16",
        "piętnaście" to "15",
        "czternaście" to "14",
        "trzynaście" to "13",
        "dwanaście" to "12",
        "jedenaście" to "11",
        "dziesiątka" to "10",
        "dziesięć" to "10",
        "dziewiątka" to "9",
        "dziewięć" to "9",
        "ósemka" to "8",
        "osiem" to "8",
        "siódemka" to "7",
        "siedem" to "7",
        "szóstka" to "6",
        "sześć" to "6",
        "piątka" to "5",
        "pięć" to "5",
        "czwórka" to "4",
        "cztery" to "4",
        "trójka" to "3",
        "trzy" to "3",
        "dwójka" to "2",
        "dwie" to "2",
        "dwa" to "2",
        "jedynka" to "1",
        "jeden" to "1",
        "zero" to "0"
    )

    fun normalize(rawText: String): String {
        val cleaned = rawText.lowercase().trim()
            .replace(Regex("[.,!?\\-]"), " ")

        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }.map { token ->
            wordToNumberMap[token] ?: token
        }

        return tokens.joinToString(" ")
    }
}
