package com.zpi.slowomir.overlay.ui.macro

import org.junit.Assert.assertEquals
import org.junit.Test

class MacroCommandFormatterTest {

    @Test
    fun `commands are described in polish`() {
        assertEquals("Przejdź na ekran główny", MacroCommandFormatter.format("NavigateHomeCommand", emptyMap()))
    }

    @Test
    fun `open app uses the application name when it is known`() {
        val args = mapOf("packageName" to "com.android.settings")
        assertEquals(
            "Otwórz aplikację Ustawienia",
            MacroCommandFormatter.format("OpenAppCommand", args) { "Ustawienia" },
        )
        assertEquals(
            "Otwórz aplikację com.android.settings",
            MacroCommandFormatter.format("OpenAppCommand", args),
        )
    }

    @Test
    fun `numeric arguments are spelled out`() {
        assertEquals("Dodaj kolumnę", MacroCommandFormatter.format("AddColumnGridCommand", mapOf("num" to 1)))
        assertEquals("Dodaj 3 kolumny", MacroCommandFormatter.format("AddColumnGridCommand", mapOf("num" to 3)))
        assertEquals("Usuń 5 wierszy", MacroCommandFormatter.format("SubtractRowGridCommand", mapOf("num" to 5)))
        assertEquals("Podziel pole na 9 części", MacroCommandFormatter.format("DivideCellCommand", mapOf("divisionPower" to 3)))
        assertEquals("Wybierz pole 7", MacroCommandFormatter.format("SelectCellCommand", mapOf("cell" to 7)))
        assertEquals(
            "Przytrzymaj zaznaczone pole przez 1,5 s",
            MacroCommandFormatter.format("LongTapCellCommand", mapOf("duration" to 1500)),
        )
    }

    @Test
    fun `macro position is shown 1-based`() {
        assertEquals("Uruchom makro 3", MacroCommandFormatter.format("PlayMacroCommand", mapOf("index" to 2)))
    }

    @Test
    fun `unknown commands fall back to a readable name`() {
        assertEquals("Some New", MacroCommandFormatter.format("SomeNewCommand", emptyMap()))
    }
}
