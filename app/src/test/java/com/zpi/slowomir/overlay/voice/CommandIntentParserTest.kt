package com.zpi.slowomir.overlay.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.voice.CommandIntentParser
import com.zpi.slowomir.voice.PolishTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CommandIntentParserTest {

    private val parser = CommandIntentParser(PolishTextNormalizer())

    @Test
    fun `powinien poprawnie rozpoznać komendę kliknięcia z cyfrą oraz ze słownym liczebnikiem`() {
        val resultNumeric = parser.parse("Kliknij 5")
        assertNotNull(resultNumeric)
        assertEquals(CommandIdentifier.CELL_PRESS, resultNumeric?.identifier)
        assertEquals(CommandArgument.Number(5), resultNumeric?.argument)

        val resultWord = parser.parse("Kliknij pięć")
        assertNotNull(resultWord)
        assertEquals(CommandIdentifier.CELL_PRESS, resultWord?.identifier)
        assertEquals(CommandArgument.Number(5), resultWord?.argument)

        val resultDotknij = parser.parse("Dotknij pola 4")
        assertNotNull(resultDotknij)
        assertEquals(CommandIdentifier.CELL_PRESS, resultDotknij?.identifier)
        assertEquals(CommandArgument.Number(4), resultDotknij?.argument)

        val resultWybierzKliknij = parser.parse("Wybierz i kliknij 7")
        assertNotNull(resultWybierzKliknij)
        assertEquals(CommandIdentifier.CELL_PRESS, resultWybierzKliknij?.identifier)
        assertEquals(CommandArgument.Number(7), resultWybierzKliknij?.argument)

        val resultLongPress = parser.parse("Przytrzymaj osiemnaście")
        assertNotNull(resultLongPress)
        assertEquals(CommandIdentifier.CELL_LONG_PRESS, resultLongPress?.identifier)
        assertEquals(CommandArgument.Number(18), resultLongPress?.argument)

        val resultDlugoNacisnij = parser.parse("Naciśnij i przytrzymaj pole 6")
        assertNotNull(resultDlugoNacisnij)
        assertEquals(CommandIdentifier.CELL_LONG_PRESS, resultDlugoNacisnij?.identifier)
        assertEquals(CommandArgument.Number(6), resultDlugoNacisnij?.argument)
    }

    @Test
    fun `powinien poprawnie rozpoznać przeciągnięcie z dwoma argumentami numerycznymi`() {
        val result = parser.parse("Przeciągnij z 2 do 8")
        assertNotNull(result)
        assertEquals(CommandIdentifier.CELL_SWIPE, result?.identifier)
        assertEquals(CommandArgument.TwoNumbers(2, 8), result?.argument)

        val resultWord = parser.parse("Przesuń od jeden do dziesięć")
        assertNotNull(resultWord)
        assertEquals(CommandIdentifier.CELL_SWIPE, resultWord?.identifier)
        assertEquals(CommandArgument.TwoNumbers(1, 10), resultWord?.argument)

        val resultGest = parser.parse("Wykonaj gest z 2 do 8")
        assertNotNull(resultGest)
        assertEquals(CommandIdentifier.CELL_SWIPE, resultGest?.identifier)
        assertEquals(CommandArgument.TwoNumbers(2, 8), resultGest?.argument)

        val resultPalcem = parser.parse("Przesuń palcem z 1 do 10")
        assertNotNull(resultPalcem)
        assertEquals(CommandIdentifier.CELL_SWIPE, resultPalcem?.identifier)
        assertEquals(CommandArgument.TwoNumbers(1, 10), resultPalcem?.argument)
    }

    @Test
    fun `powinien poprawnie rozpoznać podział i wybór pola`() {
        val divide1 = parser.parse("Rozdziel pole 3")
        assertEquals(CommandIdentifier.CELL_DIVIDE, divide1?.identifier)
        assertEquals(CommandArgument.Number(3), divide1?.argument)

        val divide2 = parser.parse("Podziel komórkę 5")
        assertEquals(CommandIdentifier.CELL_DIVIDE, divide2?.identifier)
        assertEquals(CommandArgument.Number(5), divide2?.argument)

        val select1 = parser.parse("Zaznacz pole 8")
        assertEquals(CommandIdentifier.CELL_SELECT, select1?.identifier)
        assertEquals(CommandArgument.Number(8), select1?.argument)

        val select2 = parser.parse("Wybierz pole numer 12")
        assertEquals(CommandIdentifier.CELL_SELECT, select2?.identifier)
        assertEquals(CommandArgument.Number(12), select2?.argument)
    }

    @Test
    fun `powinien poprawnie rozpoznać otwieranie aplikacji`() {
        val resultSpotify = parser.parse("Otwórz Spotify")
        assertNotNull(resultSpotify)
        assertEquals(CommandIdentifier.NAVIGATE_APP_OPEN, resultSpotify?.identifier)
        assertEquals(CommandArgument.Text("spotify"), resultSpotify?.argument)

        val resultYouTube = parser.parse("Uruchom YouTube")
        assertNotNull(resultYouTube)
        assertEquals(CommandIdentifier.NAVIGATE_APP_OPEN, resultYouTube?.identifier)
        assertEquals(CommandArgument.Text("youtube"), resultYouTube?.argument)

        val resultWlaczAplikacje = parser.parse("Włącz aplikację Spotify")
        assertNotNull(resultWlaczAplikacje)
        assertEquals(CommandIdentifier.NAVIGATE_APP_OPEN, resultWlaczAplikacje?.identifier)
        assertEquals(CommandArgument.Text("spotify"), resultWlaczAplikacje?.argument)

        val resultPrzejdzDo = parser.parse("Przejdź do aplikacji YouTube")
        assertNotNull(resultPrzejdzDo)
        assertEquals(CommandIdentifier.NAVIGATE_APP_OPEN, resultPrzejdzDo?.identifier)
        assertEquals(CommandArgument.Text("youtube"), resultPrzejdzDo?.argument)
    }

    @Test
    fun `powinien poprawnie rozpoznać komendy telefoniczne oraz SMS`() {
        val resultCall = parser.parse("Zadzwoń Jan Kowalski")
        assertNotNull(resultCall)
        assertEquals(CommandIdentifier.PHONE_CALL, resultCall?.identifier)
        assertEquals(CommandArgument.Text("jan kowalski"), resultCall?.argument)

        val resultCallDo = parser.parse("Zadzwoń do Mama")
        assertNotNull(resultCallDo)
        assertEquals(CommandIdentifier.PHONE_CALL, resultCallDo?.identifier)
        assertEquals(CommandArgument.Text("mama"), resultCallDo?.argument)

        val resultWybierzNumer = parser.parse("Wybierz numer do Jan Kowalski")
        assertNotNull(resultWybierzNumer)
        assertEquals(CommandIdentifier.PHONE_CALL, resultWybierzNumer?.identifier)
        assertEquals(CommandArgument.Text("jan kowalski"), resultWybierzNumer?.argument)

        val resultWykonajPolaczenie = parser.parse("Wykonaj połączenie do Mama")
        assertNotNull(resultWykonajPolaczenie)
        assertEquals(CommandIdentifier.PHONE_CALL, resultWykonajPolaczenie?.identifier)
        assertEquals(CommandArgument.Text("mama"), resultWykonajPolaczenie?.argument)

        val resultSms = parser.parse("SMS Anna")
        assertNotNull(resultSms)
        assertEquals(CommandIdentifier.SMS_WRITE, resultSms?.identifier)
        assertEquals(CommandArgument.Text("anna"), resultSms?.argument)

        val resultSmsDo = parser.parse("SMS do Mamy")
        assertNotNull(resultSmsDo)
        assertEquals(CommandIdentifier.SMS_WRITE, resultSmsDo?.identifier)
        assertEquals(CommandArgument.Text("mamy"), resultSmsDo?.argument)

        val resultNapiszDo = parser.parse("Napisz do Mamy")
        assertNotNull(resultNapiszDo)
        assertEquals(CommandIdentifier.SMS_WRITE, resultNapiszDo?.identifier)
        assertEquals(CommandArgument.Text("mamy"), resultNapiszDo?.argument)

        val resultNapiszSms = parser.parse("Napisz SMS do Anna")
        assertNotNull(resultNapiszSms)
        assertEquals(CommandIdentifier.SMS_WRITE, resultNapiszSms?.identifier)
        assertEquals(CommandArgument.Text("anna"), resultNapiszSms?.argument)

        val resultWyslijWiadomosc = parser.parse("Wyślij wiadomość do Piotr")
        assertNotNull(resultWyslijWiadomosc)
        assertEquals(CommandIdentifier.SMS_WRITE, resultWyslijWiadomosc?.identifier)
        assertEquals(CommandArgument.Text("piotr"), resultWyslijWiadomosc?.argument)
    }

    @Test
    fun `powinien poprawnie rozpoznać makra`() {
        val resultMacro = parser.parse("Makro Poranek")
        assertNotNull(resultMacro)
        assertEquals(CommandIdentifier.MACRO_PLAY, resultMacro?.identifier)
        assertEquals(CommandArgument.Text("poranek"), resultMacro?.argument)

        val resultUruchomMakro = parser.parse("Wykonaj makro Poranek")
        assertNotNull(resultUruchomMakro)
        assertEquals(CommandIdentifier.MACRO_PLAY, resultUruchomMakro?.identifier)
        assertEquals(CommandArgument.Text("poranek"), resultUruchomMakro?.argument)

        val resultOdtworzMakro = parser.parse("Odtwórz makro Praca")
        assertNotNull(resultOdtworzMakro)
        assertEquals(CommandIdentifier.MACRO_PLAY, resultOdtworzMakro?.identifier)
        assertEquals(CommandArgument.Text("praca"), resultOdtworzMakro?.argument)
    }

    @Test
    fun `powinien poprawnie rozpoznać synonimy komend bezargumentowych`() {
        val gridShow1 = parser.parse("Pokaż siatkę")
        assertEquals(CommandIdentifier.GRID_SHOW, gridShow1?.identifier)

        val gridShow2 = parser.parse("Wyświetl siatkę")
        assertEquals(CommandIdentifier.GRID_SHOW, gridShow2?.identifier)

        val gridShow3 = parser.parse("Wyświetl pola")
        assertEquals(CommandIdentifier.GRID_SHOW, gridShow3?.identifier)

        val gridHide = parser.parse("Schowaj siatkę")
        assertEquals(CommandIdentifier.GRID_HIDE, gridHide?.identifier)

        val stop1 = parser.parse("Wyłącz sterowanie")
        assertEquals(CommandIdentifier.OVERLAY_STOP, stop1?.identifier)

        val stop2 = parser.parse("Uśpij nakładkę")
        assertEquals(CommandIdentifier.OVERLAY_STOP, stop2?.identifier)

        val start1 = parser.parse("SłowoMIREK obudź się")
        assertEquals(CommandIdentifier.OVERLAY_START, start1?.identifier)

        val start2 = parser.parse("Włącz sterowanie SłowoMIREK")
        assertEquals(CommandIdentifier.OVERLAY_START, start2?.identifier)

        val back1 = parser.parse("Wróć do poprzedniego ekranu")
        assertEquals(CommandIdentifier.NAVIGATE_BACK, back1?.identifier)

        val home1 = parser.parse("Przejdź na ekran główny")
        assertEquals(CommandIdentifier.NAVIGATE_HOME, home1?.identifier)

        val home2 = parser.parse("Wróć do pulpitu")
        assertEquals(CommandIdentifier.NAVIGATE_HOME, home2?.identifier)

        val recents = parser.parse("Pokaż ostatnie aplikacje")
        assertEquals(CommandIdentifier.NAVIGATE_RECENT, recents?.identifier)

        val notifs1 = parser.parse("Panel powiadomień")
        assertEquals(CommandIdentifier.NOTIFICATIONS, notifs1?.identifier)

        val notifs2 = parser.parse("Rozwiń powiadomienia")
        assertEquals(CommandIdentifier.NOTIFICATIONS, notifs2?.identifier)

        val quickSettings = parser.parse("Panel szybkich ustawień")
        assertEquals(CommandIdentifier.QUICK_SETTINGS, quickSettings?.identifier)

        val lock = parser.parse("Zgaś i zablokuj ekran")
        assertEquals(CommandIdentifier.LOCK_SCREEN, lock?.identifier)

        val screenshot1 = parser.parse("Zrób zrzut ekranu")
        assertEquals(CommandIdentifier.SCREENSHOT, screenshot1?.identifier)

        val screenshot2 = parser.parse("Przechwyć ekran")
        assertEquals(CommandIdentifier.SCREENSHOT, screenshot2?.identifier)

        val undivide = parser.parse("Połącz podzielone pole")
        assertEquals(CommandIdentifier.CELL_UNDIVIDE, undivide?.identifier)

        val resetGrid1 = parser.parse("Zresetuj siatkę")
        assertEquals(CommandIdentifier.GRID_RESET, resetGrid1?.identifier)

        val resetGrid2 = parser.parse("Wyczyść podziały siatki")
        assertEquals(CommandIdentifier.GRID_RESET, resetGrid2?.identifier)

        val volUp = parser.parse("Podgłośnij")
        assertEquals(CommandIdentifier.VOLUME_UP, volUp?.identifier)

        val volDown = parser.parse("Ścisz")
        assertEquals(CommandIdentifier.VOLUME_DOWN, volDown?.identifier)

        val pause = parser.parse("Zatrzymaj odtwarzanie")
        assertEquals(CommandIdentifier.MEDIA_PAUSE, pause?.identifier)

        val play = parser.parse("Wznów odtwarzanie")
        assertEquals(CommandIdentifier.MEDIA_PLAY, play?.identifier)

        val skipFwd = parser.parse("Przeskocz do przodu")
        assertEquals(CommandIdentifier.MEDIA_SKIP_FORWARD, skipFwd?.identifier)

        val skipBwd = parser.parse("Przeskocz do tyłu")
        assertEquals(CommandIdentifier.MEDIA_SKIP_BACKWARD, skipBwd?.identifier)

        val nextTrack = parser.parse("Następny utwór")
        assertEquals(CommandIdentifier.MEDIA_NEXT_TRACK, nextTrack?.identifier)

        val prevTrack = parser.parse("Wcześniejsza piosenka")
        assertEquals(CommandIdentifier.MEDIA_PREVIOUS_TRACK, prevTrack?.identifier)

        val dictation1 = parser.parse("Rozpocznij dyktowanie")
        assertEquals(CommandIdentifier.TEXT_ENTER, dictation1?.identifier)

        val dictation2 = parser.parse("Wpisz tekst głosem")
        assertEquals(CommandIdentifier.TEXT_ENTER, dictation2?.identifier)

        val hideKb = parser.parse("Schowaj klawiaturę ekranową")
        assertEquals(CommandIdentifier.HIDE_KEYBOARD, hideKb?.identifier)

        val showKb = parser.parse("Wyświetl klawiaturę")
        assertEquals(CommandIdentifier.SHOW_KEYBOARD, showKb?.identifier)
    }
}
