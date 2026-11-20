package com.zpi.slowomir.prediction

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionGuardTest {

    @Test
    fun `isGuardedPackage blocks sensitive apps`() {
        assertTrue(PredictionGuard.isGuardedPackage("pl.mbank"))
        assertTrue(PredictionGuard.isGuardedPackage("pl.pkobp.iko"))
        assertTrue(PredictionGuard.isGuardedPackage("com.x8bit.bitwarden"))
        assertTrue(PredictionGuard.isGuardedPackage("pl.nask.mobywatel"))

        assertFalse(PredictionGuard.isGuardedPackage("com.google.android.youtube"))
        assertFalse(PredictionGuard.isGuardedPackage("com.whatsapp"))
    }

    @Test
    fun `isProtectedLabel detects dangerous action roots`() {
        val dangerous = listOf(
            "usuń",
            "Usuń wiadomość",
            "Skasuj",
            "Wykasuj pliki",
            "Wyślij",
            "Kup bilet",
            "Zapłać teraz",
            "Przelej środki",
            "Zatwierdź przelew",
            "Formatuj",
            "Zresetuj ustawienia",
        )

        for (text in dangerous) {
            val normalized = PredictionGuard.normalizeText(text)
            assertTrue("Expected protected label for '$text' (normalized: '$normalized')", PredictionGuard.isProtectedLabel(normalized))
        }
    }

    @Test
    fun `isProtectedLabel allows safe navigation labels`() {
        val safe = listOf(
            "Szukaj",
            "Biblioteka",
            "Wstecz",
            "Dalej",
            "Otwórz",
            "Czat",
            "Ustawienia",
        )

        for (text in safe) {
            val normalized = PredictionGuard.normalizeText(text)
            assertFalse("Expected safe label for '$text' (normalized: '$normalized')", PredictionGuard.isProtectedLabel(normalized))
        }
    }
}
