package com.zpi.slowomir.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class PolishTextNormalizerTest {

    private val normalizer = PolishTextNormalizer()

    @Test
    fun `normalizes all 0 to 20 words and their inflected forms`() {
        val expectedMappings = mapOf(
            // 20
            "dwadzieścia" to "20",
            "dwudziesty" to "20",
            "dwudziesta" to "20",
            "dwudzieste" to "20",
            "dwudziestego" to "20",
            "dwudziestą" to "20",

            // 19
            "dziewiętnaście" to "19",
            "dziewiętnasty" to "19",
            "dziewiętnasta" to "19",
            "dziewiętnaste" to "19",

            // 18
            "osiemnaście" to "18",
            "osiemnasty" to "18",
            "osiemnasta" to "18",
            "osiemnaste" to "18",

            // 17
            "siedemnaście" to "17",
            "siedemnasty" to "17",
            "siedemnasta" to "17",
            "siedemnaste" to "17",

            // 16
            "szesnaście" to "16",
            "szesnasty" to "16",
            "szesnasta" to "16",
            "szesnaste" to "16",

            // 15
            "piętnaście" to "15",
            "piętnasty" to "15",
            "piętnasta" to "15",
            "piętnaste" to "15",

            // 14
            "czternaście" to "14",
            "czternasty" to "14",
            "czternasta" to "14",
            "czternaste" to "14",

            // 13
            "trzynaście" to "13",
            "trzynasty" to "13",
            "trzynasta" to "13",
            "trzynaste" to "13",

            // 12
            "dwanaście" to "12",
            "dwunasty" to "12",
            "dwunasta" to "12",
            "dwunaste" to "12",

            // 11
            "jedenaście" to "11",
            "jedenasty" to "11",
            "jedenasta" to "11",
            "jedenaste" to "11",

            // 10
            "dziesięć" to "10",
            "dziesiątka" to "10",
            "dziesiątkę" to "10",
            "dziesiąty" to "10",
            "dziesiąta" to "10",
            "dziesiąte" to "10",
            "dziesiątego" to "10",
            "dziesiątą" to "10",

            // 9
            "dziewięć" to "9",
            "dziewiątka" to "9",
            "dziewiątkę" to "9",
            "dziewiąty" to "9",
            "dziewiąta" to "9",
            "dziewiąte" to "9",
            "dziewiątego" to "9",
            "dziewiątą" to "9",

            // 8
            "osiem" to "8",
            "ósemka" to "8",
            "ósemkę" to "8",
            "ósmy" to "8",
            "ósma" to "8",
            "ósme" to "8",
            "ósmego" to "8",
            "ósmą" to "8",

            // 7
            "siedem" to "7",
            "siódemka" to "7",
            "siódemkę" to "7",
            "siódmy" to "7",
            "siódma" to "7",
            "siódme" to "7",
            "siódmego" to "7",
            "siódmą" to "7",

            // 6
            "sześć" to "6",
            "szóstka" to "6",
            "szóstkę" to "6",
            "szósty" to "6",
            "szósta" to "6",
            "szóste" to "6",
            "szóstego" to "6",
            "szóstą" to "6",

            // 5
            "pięć" to "5",
            "piątka" to "5",
            "piątkę" to "5",
            "piąty" to "5",
            "piąta" to "5",
            "piąte" to "5",
            "piątego" to "5",
            "piątą" to "5",

            // 4
            "cztery" to "4",
            "czwórka" to "4",
            "czwórkę" to "4",
            "czwarty" to "4",
            "czwarta" to "4",
            "czwarte" to "4",
            "czwartego" to "4",
            "czwartą" to "4",

            // 3
            "trzy" to "3",
            "trójka" to "3",
            "trójkę" to "3",
            "trzeci" to "3",
            "trzecia" to "3",
            "trzecie" to "3",
            "trzeciego" to "3",
            "trzecią" to "3",

            // 2
            "dwa" to "2",
            "dwie" to "2",
            "dwójka" to "2",
            "dwójkę" to "2",
            "drugi" to "2",
            "druga" to "2",
            "drugie" to "2",
            "drugiego" to "2",
            "drugą" to "2",

            // 1
            "jeden" to "1",
            "jedynka" to "1",
            "jedynkę" to "1",
            "pierwszy" to "1",
            "pierwsza" to "1",
            "pierwsze" to "1",
            "pierwszego" to "1",
            "pierwszą" to "1",

            // 0
            "zero" to "0"
        )

        for ((word, expectedDigit) in expectedMappings) {
            val actual = normalizer.normalize(word)
            assertEquals("Word '$word' should normalize to '$expectedDigit'", expectedDigit, actual)
        }
    }

    @Test
    fun `normalizes mixed phrases with numbers and punctuation`() {
        assertEquals("kliknij 1 pole", normalizer.normalize("Kliknij pierwsze pole!"))
        assertEquals("wybierz 2", normalizer.normalize("Wybierz drugą."))
        assertEquals("przesuń od 3 do 5", normalizer.normalize("Przesuń od trzeciego do piątego"))
        assertEquals("zadzwoń pod 20", normalizer.normalize("Zadzwoń pod dwudziesty..."))
        assertEquals("wybierz opcję 10", normalizer.normalize("Wybierz opcję dziesiątą!"))
    }

    @Test
    fun `leaves unknown words and numbers unchanged`() {
        assertEquals("kontakt jan kowalski", normalizer.normalize("Kontakt Jan Kowalski"))
        assertEquals("123", normalizer.normalize("123"))
    }
}
