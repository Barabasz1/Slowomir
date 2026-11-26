package com.zpi.slowomir.overlay.ui.macro

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.macro.MacroStartingPoint
import java.util.Locale


object MacroCommandFormatter {


    fun format(command: Command, appLabel: (String) -> String? = { null }): String =
        format(command::class.java.simpleName, command.arguments, appLabel)

    fun format(
        commandName: String,
        arguments: Map<String, Any?>,
        appLabel: (String) -> String? = { null },
    ): String {
        val args = Args(arguments)
        return when (commandName.removeSuffix("Command")) {
            // Grid
            "GridEnable" -> "Pokaż siatkę"
            "GridDisable" -> "Ukryj siatkę"
            "GridToggle" -> "Przełącz siatkę"
            "GridReset" -> "Zresetuj siatkę"
            "AddColumnGrid" -> "Dodaj ${counted(args.int("num", 1), "kolumnę", "kolumny", "kolumn")}"
            "AddRowGrid" -> "Dodaj ${counted(args.int("num", 1), "wiersz", "wiersze", "wierszy")}"
            "SubtractColumnGrid" -> "Usuń ${counted(args.int("num", 1), "kolumnę", "kolumny", "kolumn")}"
            "SubtractRowGrid" -> "Usuń ${counted(args.int("num", 1), "wiersz", "wiersze", "wierszy")}"
            "SelectCell" -> "Wybierz pole ${args.int("cell", 1)}"
            "DivideCell" -> {
                val power = args.int("divisionPower", 2)
                "Podziel pole na ${power * power} części"
            }
            "UndivideCell" -> "Cofnij podział pola"
            "TapCell" -> args.string("targetLabel")?.let { "Kliknij „$it”" } ?: "Kliknij zaznaczone pole"
            "LongTapCell" -> (args.string("targetLabel")?.let { "Przytrzymaj „$it”" } ?: "Przytrzymaj zaznaczone pole") +
                    forSeconds(args.long("duration"))
            "SwipeCell" -> "Przeciągnij z pola ${args.int("cell0", 1)} do pola ${args.int("cell1", 2)}"
            "SwipeUpCell" -> "Przesuń w górę z zaznaczonego pola${byForce(args.float("force")?.toInt())}"
            "SwipeDownCell" -> "Przesuń w dół z zaznaczonego pola${byForce(args.float("force")?.toInt())}"
            "SwipeLeftCell" -> "Przesuń w lewo z zaznaczonego pola${byForce(args.float("force")?.toInt())}"
            "SwipeRightCell" -> "Przesuń w prawo z zaznaczonego pola${byForce(args.float("force")?.toInt())}"
            "ZoomInCell" -> "Przybliż zaznaczone pole"
            "ZoomOutCell" -> "Oddal zaznaczone pole"

            // Navigation
            "NavigateHome" -> "Przejdź na ekran główny"
            "NavigateBack" -> "Wróć"
            "NavigateRecent" -> "Pokaż ostatnie aplikacje"
            "Notifications" -> "Pokaż powiadomienia"
            "QuickSettings" -> "Pokaż szybkie ustawienia"
            "LockScreen" -> "Zablokuj ekran"
            "Screenshot" -> "Zrób zrzut ekranu"
            "OpenApp" -> {
                val pkg = args.string("packageName")
                if (pkg == null) "Otwórz aplikację" else "Otwórz aplikację ${appLabel(pkg) ?: pkg}"
            }
            "SwipeUpNav" -> "Przewiń w górę"
            "SwipeDownNav" -> "Przewiń w dół"
            "SwipeLeftNav" -> "Przewiń w lewo"
            "SwipeRightNav" -> "Przewiń w prawo"
            "ZoomInNav" -> "Przybliż"
            "ZoomOutNav" -> "Oddal"

            // Touch (raw gestures)
            "Tap" -> "Dotknij ekranu${atPoint(args)}"
            "LongTap" -> "Przytrzymaj palec na ekranie${atPoint(args)}${forSeconds(args.long("duration"))}"
            "Swipe" -> "Przeciągnij palcem po ekranie"
            "SwipeUp" -> "Przesuń palcem w górę${atPoint(args)}${byForce(args.float("force")?.toInt())}"
            "SwipeDown" -> "Przesuń palcem w dół${atPoint(args)}${byForce(args.float("force")?.toInt())}"
            "SwipeLeft" -> "Przesuń palcem w lewo${atPoint(args)}${byForce(args.float("force")?.toInt())}"
            "SwipeRight" -> "Przesuń palcem w prawo${atPoint(args)}${byForce(args.float("force")?.toInt())}"
            "PinchIn" -> "Ściśnij palce${atPoint(args)}"
            "PinchOut" -> "Rozsuń palce${atPoint(args)}"

            // Media
            "Play" -> "Wznów odtwarzanie"
            "Pause" -> "Wstrzymaj odtwarzanie"
            "VolumeUp" -> "Zwiększ głośność"
            "VolumeDown" -> "Zmniejsz głośność"

            // Text
            "Enter" -> "Zacznij dyktować"
            "Edit" -> "Przejdź do edycji tekstu"
            "Confirm" -> "Zatwierdź tekst"
            "Cancel" -> "Anuluj pisanie"
            "Dictate" -> "Wpisz tekst"
            "CursorForward" -> "Kursor o znak w prawo"
            "CursorBackward" -> "Kursor o znak w lewo"
            "CursorWordNext" -> "Kursor o słowo w prawo"
            "CursorWordPrevious" -> "Kursor o słowo w lewo"
            "DeleteChar" -> "Usuń znak"
            "DeleteWord" -> "Usuń słowo"
            "Punctuation" -> when (args.string("mark")) {
                "." -> "Wstaw kropkę"
                "," -> "Wstaw przecinek"
                "?" -> "Wstaw znak zapytania"
                "!" -> "Wstaw wykrzyknik"
                null -> "Wstaw znak interpunkcyjny"
                else -> "Wstaw znak „${args.string("mark")}”"
            }
            "KeyboardShow" -> "Pokaż klawiaturę"
            "KeyboardHide" -> "Schowaj klawiaturę"
            "KeyboardToggle" -> "Przełącz klawiaturę"
            "Dismiss" -> "Odrzuć propozycję"

            // Phone
            "PickUp" -> "Odbierz połączenie"
            "Decline" -> "Odrzuć połączenie"
            "End" -> "Zakończ połączenie"
            "Speaker" -> "Przełącz głośnik rozmowy"
            "Call" -> args.string("recipient")?.let { "Zadzwoń pod numer $it" } ?: "Zadzwoń"
            "WriteSMS" -> args.string("recipient")?.let { "Napisz SMS do $it" } ?: "Napisz SMS"

            // Macros
            "PlayMacro" -> "Uruchom makro ${args.int("index", 0) + 1}"
            "StopMacro" -> "Zatrzymaj makro"
            "DeleteLastRecordedAction" -> "Usuń ostatnią akcję"

            else -> humanize(commandName)
        }
    }

    fun formatStartingPoint(startingPoint: MacroStartingPoint?, appLabel: (String) -> String? = { null }): String? =
        when (startingPoint) {
            null -> null
            MacroStartingPoint.Home -> "Ekran startowy"
            is MacroStartingPoint.App -> {
                val app = appLabel(startingPoint.appName) ?: startingPoint.appName
                if (startingPoint.section == null) app else "$app/${startingPoint.section}"
            }
        }

    private fun humanize(commandName: String): String =
        commandName.removeSuffix("Command").replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

    private fun counted(n: Int, one: String, few: String, many: String): String = when {
        n == 1 -> one
        n % 10 in 2..4 && n % 100 !in 12..14 -> "$n $few"
        else -> "$n $many"
    }

    private fun forSeconds(durationMs: Long?): String =
        if (durationMs == null || durationMs <= 0) "" else " przez ${seconds(durationMs)} s"

    private fun seconds(durationMs: Long): String {
        val s = durationMs / 1000f
        return if (s % 1f == 0f) s.toInt().toString() else "%.1f".format(Locale("pl"), s)
    }

    private fun byForce(force: Int?): String =
        if (force == null) "" else " (siła $force)"

    private fun atPoint(args: Args): String {
        val x = args.float("x") ?: return ""
        val y = args.float("y") ?: return ""
        return " w punkcie (${x.toInt()}, ${y.toInt()})"
    }

    private class Args(private val map: Map<String, Any?>) {
        fun string(key: String): String? = map[key]?.toString()?.takeIf { it.isNotBlank() }

        fun int(key: String, default: Int): Int = when (val v = map[key]) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }

        fun long(key: String): Long? = (map[key] as? Number)?.toLong()

        fun float(key: String): Float? = (map[key] as? Number)?.toFloat()
    }
}
