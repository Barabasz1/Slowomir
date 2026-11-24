package com.zpi.slowomir.overlay.ui.macro

import com.zpi.slowomir.overlay.macro.MacroStartingPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroCommandFormatterLabelTest {

    @Test
    fun `tap shows the name of the clicked element`() {
        assertEquals(
            "Kliknij „Ustawienia”",
            MacroCommandFormatter.format("TapCellCommand", mapOf("targetLabel" to "Ustawienia")),
        )
        assertEquals("Kliknij zaznaczone pole", MacroCommandFormatter.format("TapCellCommand", emptyMap()))
    }
}

class MacroStartingPointFormatTest {
    private val labels = mapOf("com.google.android.youtube" to "YouTube")

    @Test
    fun `starting point is shown as the app and its section`() {
        val app = MacroStartingPoint.App("com.google.android.youtube", "Profil")
        assertEquals("YouTube/Profil", MacroCommandFormatter.formatStartingPoint(app) { labels[it] })
        assertEquals(
            "YouTube",
            MacroCommandFormatter.formatStartingPoint(MacroStartingPoint.App("com.google.android.youtube")) { labels[it] },
        )
    }

    @Test
    fun `home and missing starting points`() {
        assertEquals("Ekran startowy", MacroCommandFormatter.formatStartingPoint(MacroStartingPoint.Home))
        assertNull(MacroCommandFormatter.formatStartingPoint(null))
    }
}
