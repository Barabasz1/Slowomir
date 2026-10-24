package com.zpi.slowomir.overlay.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import javax.inject.Inject

/**
 * Performs an analysis of normalized speech input and transforms it into structurized [VoiceIntent]
 * containing command identifier and argument.
 */
class CommandIntentParser @Inject constructor(
    private val normalizer: PolishTextNormalizer
) {

    private val swipeTwoNumbersRegex = Regex(
        pattern = """^(?:przeciągnij|przesuń|wykonaj\s+gest|przesuń\s+palcem)\s+(?:z|od)?\s*(\d+)\s+do\s+(\d+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val pressNumberRegex = Regex(
        pattern = """^(?:kliknij|naciśnij|dotknij|wybierz\s+i\s+kliknij)\s+(?:pola|pole|numer)?\s*(\d+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val longPressNumberRegex = Regex(
        pattern = """^(?:naciśnij\s+i\s+przytrzymaj|długo\s+naciśnij|przytrzymaj|wykonaj\s+długie\s+naciśnięcie\s+na)\s+(?:pola|pole|numer)?\s*(\d+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val divideNumberRegex = Regex(
        pattern = """^(?:podziel|rozdziel)\s+(?:pola|pole|komórkę|numer)?\s*(\d+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val selectNumberRegex = Regex(
        pattern = """^(?:wybierz\s+pole\s+numer|wybierz\s+pole|wybierz|zaznacz\s+pole|pole)?\s*(\d+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val openAppRegex = Regex(
        pattern = """^(?:otwórz(?:\s+aplikację)?|uruchom|włącz(?:\s+aplikację)?|przejdź\s+do\s+aplikacji)\s+(.+)""",
        option = RegexOption.IGNORE_CASE
    )

    private val phoneCallRegex = Regex(
        pattern = """^(?:zadzwoń(?:\s+do)?|połącz\s+z|wybierz\s+numer\s+do|wykonaj\s+połączenie\s+do)\s+(.+)""",
        option = RegexOption.IGNORE_CASE
    )

    private val smsWriteRegex = Regex(
        pattern = """^(?:sms|napisz\s+sms\s+do|wyślij\s+wiadomość\s+do|utwórz\s+sms\s+do|napisz\s+wiadomość\s+tekstową\s+do|napisz\s+do)\s+(.+)""",
        option = RegexOption.IGNORE_CASE
    )

    private val macroPlayRegex = Regex(
        pattern = """^(?:uruchom\s+makro|wykonaj\s+makro|odtwórz\s+makro|włącz\s+makro|makro)\s+(.+)""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(rawInput: String): VoiceIntent? {
        val text = normalizer.normalize(rawInput)
        if (text.isBlank()) return null

        // 1. Two-number patterns (CELL_SWIPE)
        swipeTwoNumbersRegex.find(text)?.let { match ->
            val num1 = match.groupValues[1].toIntOrNull()
            val num2 = match.groupValues[2].toIntOrNull()
            if (num1 != null && num2 != null) {
                return VoiceIntent(
                    identifier = CommandIdentifier.CELL_SWIPE,
                    argument = CommandArgument.TwoNumbers(num1, num2),
                    rawText = rawInput
                )
            }
        }

        // 2. Single-number patterns
        pressNumberRegex.find(text)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { num ->
                return VoiceIntent(
                    identifier = CommandIdentifier.CELL_PRESS,
                    argument = CommandArgument.Number(num),
                    rawText = rawInput
                )
            }
        }

        longPressNumberRegex.find(text)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { num ->
                return VoiceIntent(
                    identifier = CommandIdentifier.CELL_LONG_PRESS,
                    argument = CommandArgument.Number(num),
                    rawText = rawInput
                )
            }
        }

        divideNumberRegex.find(text)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { num ->
                return VoiceIntent(
                    identifier = CommandIdentifier.CELL_DIVIDE,
                    argument = CommandArgument.Number(num),
                    rawText = rawInput
                )
            }
        }

        selectNumberRegex.find(text)?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { num ->
                return VoiceIntent(
                    identifier = CommandIdentifier.CELL_SELECT,
                    argument = CommandArgument.Number(num),
                    rawText = rawInput
                )
            }
        }

        // 3. Zero-argument commands (exact phrase match - must be checked before greedy text argument patterns)
        val noArgIdentifier = when (text) {
            "pokaż siatkę", "wyświetl siatkę", "włącz siatkę", "pokaż podział ekranu", "wyświetl pola" -> CommandIdentifier.GRID_SHOW
            "ukryj siatkę", "schowaj siatkę", "wyłącz siatkę", "usuń siatkę z ekranu", "nie pokazuj siatki" -> CommandIdentifier.GRID_HIDE
            "wyłącz", "wyłącz sterowanie", "uśpij nakładkę", "zatrzymaj sterowanie głosowe", "przejdź w stan uśpienia", "uśpij" -> CommandIdentifier.OVERLAY_STOP
            "słowomirek powstań", "słowomirek obudź się", "słowomirek wstań", "słowomirek aktywuj się", "włącz sterowanie słowomirek", "powstań" -> CommandIdentifier.OVERLAY_START
            "przewiń w górę", "przesuń w górę", "przewijaj w górę", "w górę", "przesuń ekran do góry", "do góry" -> CommandIdentifier.SWIPE_UP
            "przewiń w dół", "przesuń w dół", "przewijaj w dół", "w dół", "przesuń ekran na dół", "do dołu" -> CommandIdentifier.SWIPE_DOWN
            "przewiń w prawo", "przesuń w prawo", "przewijaj w prawo", "w prawo", "przesuń ekran w prawo" -> CommandIdentifier.SWIPE_RIGHT
            "przewiń w lewo", "przesuń w lewo", "przewijaj w lewo", "w lewo", "przesuń ekran w lewo" -> CommandIdentifier.SWIPE_LEFT
            "przybliżyć", "przybliż", "powiększ", "zwiększ powiększenie", "zbliż widok" -> CommandIdentifier.ZOOM_IN
            "oddalić", "oddal", "pomniejsz", "zmniejsz powiększenie", "oddal widok" -> CommandIdentifier.ZOOM_OUT
            "cofnij", "wróć", "wstecz", "wróć do poprzedniego ekranu", "przejdź wstecz" -> CommandIdentifier.NAVIGATE_BACK
            "ekran startowy", "przejdź na ekran główny", "ekran główny", "wróć do pulpitu", "przejdź do ekranu startowego", "główny ekran" -> CommandIdentifier.NAVIGATE_HOME
            "ostatnie aplikacje", "pokaż ostatnie aplikacje", "otwórz ostatnie aplikacje", "ostatnio używane aplikacje", "pokaż otwarte aplikacje", "ostatnie" -> CommandIdentifier.NAVIGATE_RECENT
            "powiadomienia", "pokaż powiadomienia", "otwórz powiadomienia", "panel powiadomień", "rozwiń powiadomienia" -> CommandIdentifier.NOTIFICATIONS
            "szybkie ustawienia", "pokaż szybkie ustawienia", "otwórz szybkie ustawienia", "rozwiń szybkie ustawienia", "panel szybkich ustawień" -> CommandIdentifier.QUICK_SETTINGS
            "zablokuj", "zablokuj ekran", "zgaś i zablokuj ekran", "włącz blokadę ekranu", "zamknij ekran" -> CommandIdentifier.LOCK_SCREEN
            "zrzut ekranu", "zrób zrzut ekranu", "zrób screenshota", "zapisz zrzut ekranu", "przechwyć ekran", "screen" -> CommandIdentifier.SCREENSHOT
            "cofnij podział", "wróć do poprzedniego podziału", "usuń ostatni podział", "połącz podzielone pole" -> CommandIdentifier.CELL_UNDIVIDE
            "reset siatki", "zresetuj siatkę", "wyczyść podziały siatki", "przywróć siatkę", "zacznij siatkę od nowa", "resetuj siatkę" -> CommandIdentifier.GRID_RESET
            "kliknij", "naciśnij", "dotknij", "wybierz zaznaczone pole", "kliknij zaznaczone", "stuknij" -> CommandIdentifier.CELL_PRESS
            "przytrzymaj", "naciśnij i przytrzymaj", "długo naciśnij", "przytrzymaj zaznaczone pole", "wykonaj długie naciśnięcie" -> CommandIdentifier.CELL_LONG_PRESS
            "pauza", "wstrzymaj", "zatrzymaj odtwarzanie", "zapauzuj", "wstrzymaj odtwarzanie", "zatrzymaj" -> CommandIdentifier.MEDIA_PAUSE
            "kontynuuj", "wznów", "odtwarzaj dalej", "włącz odtwarzanie", "wznów odtwarzanie", "odtwarzaj", "graj" -> CommandIdentifier.MEDIA_PLAY
            "głośniej", "zwiększ głośność", "podgłośnij", "daj głośniej", "podnieś głośność" -> CommandIdentifier.VOLUME_UP
            "ciszej", "zmniejsz głośność", "ścisz", "przycisz", "obniż głośność" -> CommandIdentifier.VOLUME_DOWN
            "przewiń do przodu", "przeskocz do przodu", "przesuń odtwarzanie do przodu", "przewiń nagranie w przód", "przeskocz kilka sekund naprzód" -> CommandIdentifier.MEDIA_SKIP_FORWARD
            "przewiń do tyłu", "przeskocz do tyłu", "przesuń odtwarzanie do tyłu", "przewiń nagranie wstecz", "cofnij nagranie o kilka sekund" -> CommandIdentifier.MEDIA_SKIP_BACKWARD
            "następna piosenka", "następny utwór", "przejdź do następnej piosenki", "włącz następny utwór", "kolejna piosenka" -> CommandIdentifier.MEDIA_NEXT_TRACK
            "poprzednia piosenka", "poprzedni utwór", "przejdź do poprzedniej piosenki", "włącz poprzedni utwór", "wcześniejsza piosenka" -> CommandIdentifier.MEDIA_PREVIOUS_TRACK
            "potwierdź", "zaakceptuj propozycję", "wykonaj sugerowaną akcję", "potwierdź sugestię", "zgadzam się na propozycję" -> CommandIdentifier.PREDICTION_CONFIRM
            "odrzuć", "odrzuć propozycję", "nie wykonuj sugestii", "anuluj sugerowaną akcję", "pomiń sugestię" -> CommandIdentifier.PREDICTION_DISMISS
            "pisz", "dyktuj", "rozpocznij dyktowanie", "wpisz tekst głosem", "zacznij pisać" -> CommandIdentifier.TEXT_ENTER
            "do przodu", "przesuń kursor do przodu", "kursor w prawo", "jeden znak w prawo", "przesuń o znak do przodu" -> CommandIdentifier.TEXT_CURSOR_FORWARD
            "do tyłu", "przesuń kursor do tyłu", "kursor w lewo", "jeden znak w lewo", "przesuń o znak wstecz" -> CommandIdentifier.TEXT_CURSOR_BACKWARD
            "następne słowo", "przejdź do następnego słowa", "kursor o słowo w prawo", "przesuń kursor do kolejnego słowa", "o słowo do przodu" -> CommandIdentifier.TEXT_CURSOR_NEXT_WORD
            "poprzednie słowo", "przejdź do poprzedniego słowa", "kursor o słowo w lewo", "przesuń kursor do wcześniejszego słowa", "o słowo do tyłu" -> CommandIdentifier.TEXT_CURSOR_PREVIOUS_WORD
            "usuń", "usuń znak", "skasuj znak", "wykasuj literę", "usuń poprzedni znak" -> CommandIdentifier.TEXT_DELETE_CHARACTER
            "usuń słowo", "skasuj słowo", "usuń poprzednie słowo", "wykasuj słowo", "usuń wyraz" -> CommandIdentifier.TEXT_DELETE_WORD
            "zatwierdź", "zatwierdź tekst", "potwierdź wpisany tekst", "zakończ wpisywanie", "zaakceptuj tekst" -> CommandIdentifier.TEXT_CONFIRM
            "schowaj klawiaturę", "ukryj klawiaturę", "zamknij klawiaturę", "wyłącz klawiaturę ekranową", "schowaj klawiaturę ekranową" -> CommandIdentifier.HIDE_KEYBOARD
            "pokaż klawiaturę", "wyświetl klawiaturę", "otwórz klawiaturę", "włącz klawiaturę ekranową", "pokaż klawiaturę ekranową" -> CommandIdentifier.SHOW_KEYBOARD
            "odbierz", "odbierz telefon", "odbierz połączenie", "przyjmij połączenie", "odbierz rozmowę" -> CommandIdentifier.PHONE_PICK_UP
            "odrzuć połączenie", "nie odbieraj", "odmów połączenia", "odrzuć rozmowę" -> CommandIdentifier.PHONE_DECLINE
            "rozłącz", "zakończ połączenie", "rozłącz się", "zakończ rozmowę", "odłóż słuchawkę" -> CommandIdentifier.PHONE_END_CALL
            "głośnomówiący", "przełącz tryb głośnomówiący", "przełącz głośnik rozmowy", "włącz lub wyłącz głośnik rozmowy", "zmień tryb głośnomówiący" -> CommandIdentifier.PHONE_SPEAKER
            "sms" -> CommandIdentifier.SMS_WRITE
            "nagraj makro", "rozpocznij nagrywanie makra", "zacznij nagrywać makro", "utwórz nowe makro", "zapisuj czynności jako makro" -> CommandIdentifier.MACRO_RECORD
            "zakończ makro", "zakończ nagrywanie makra", "zatrzymaj nagrywanie makra", "zapisz makro", "skończ nagrywać makro" -> CommandIdentifier.MACRO_FINISH
            "makro", "pokaż makra", "otwórz listę makr", "wyświetl zapisane makra", "lista makr" -> CommandIdentifier.MACRO_PLAY
            else -> null
        }

        if (noArgIdentifier != null) {
            return VoiceIntent(
                identifier = noArgIdentifier,
                argument = null,
                rawText = rawInput
            )
        }

        // 4. Text argument patterns
        openAppRegex.find(text)?.let { match ->
            val appName = match.groupValues[1].trim()
            if (appName.isNotEmpty()) {
                return VoiceIntent(
                    identifier = CommandIdentifier.NAVIGATE_APP_OPEN,
                    argument = CommandArgument.Text(appName),
                    rawText = rawInput
                )
            }
        }

        phoneCallRegex.find(text)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) {
                return VoiceIntent(
                    identifier = CommandIdentifier.PHONE_CALL,
                    argument = CommandArgument.Text(name),
                    rawText = rawInput
                )
            }
        }

        smsWriteRegex.find(text)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) {
                return VoiceIntent(
                    identifier = CommandIdentifier.SMS_WRITE,
                    argument = CommandArgument.Text(name),
                    rawText = rawInput
                )
            }
        }

        macroPlayRegex.find(text)?.let { match ->
            val macroName = match.groupValues[1].trim()
            if (macroName.isNotEmpty()) {
                return VoiceIntent(
                    identifier = CommandIdentifier.MACRO_PLAY,
                    argument = CommandArgument.Text(macroName),
                    rawText = rawInput
                )
            }
        }

        return null
    }
}
