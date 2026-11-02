package com.zpi.slowomir.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.Log
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.util.Locale

data class ContactOption(val name: String, val number: String)

sealed class ContactLookupResult {
    data class Found(val name: String?, val number: String) : ContactLookupResult()
    data class Ambiguous(val options: List<ContactOption>) : ContactLookupResult()
    object NotFound : ContactLookupResult()
    object PermissionDenied : ContactLookupResult()
    object EmptyQuery : ContactLookupResult()
}

object ContactUtils {

    private const val TAG = "ContactUtils"
    private const val MAX_OPTIONS = 5

    // TODO
    private const val USE_TEST_CONTACTS = true
    private val TEST_CONTACTS = listOf(
        "Mama Ania" to "600100001",
        "Mama Basia" to "600100002",
    )

    private val PREPOSITIONS = setOf("do", "z", "ze", "dla", "od")
    private val PHONE_CHARS = Regex("""^[\d+\-() ]+$""")
    private val POLISH = Locale.forLanguageTag("pl")
    private val ENDINGS = listOf(
        "iego", "iemu", "iej", "ego", "emu", "iem", "ami", "ach", "owi", "ich", "ych",
        "ej", "om", "em", "ie", "ia", "ii", "im", "ym",
        "a", "e", "i", "y", "u", "o",
    )
    private data class Entry(
        val name: String,
        val tokens: List<String>,
        val number: String,
        val isMobile: Boolean,
    )

    fun lookup(context: Context, input: String): ContactLookupResult {
        val query = input.trim()
            .split(Regex("\\s+"))
            .dropWhile { fold(it) in PREPOSITIONS }
            .joinToString(" ")
        if (query.isBlank()) return ContactLookupResult.EmptyQuery

        // Sam numer: nie szukamy w kontaktach.
        if (PHONE_CHARS.matches(query)) {
            return ContactLookupResult.Found(null, query.filter { it.isDigit() || it == '+' })
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_CONTACTS not granted")
            return ContactLookupResult.PermissionDenied
        }

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return ContactLookupResult.EmptyQuery

        val scored = try {
            loadContacts(context).mapNotNull { e -> score(queryTokens, e)?.let { e to it } }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact '$query'", e)
            return ContactLookupResult.NotFound
        }
        if (scored.isEmpty()) return ContactLookupResult.NotFound

        val best = scored.maxOf { it.second }
        val top = scored.filter { it.second == best }.map { it.first }

        val byName = top.groupBy { it.name }
        if (byName.size > 1) {
            val options = byName
                .map { (name, entries) -> ContactOption(name, preferredNumber(entries)) }
                .sortedBy { it.name }
                .take(MAX_OPTIONS)
            return ContactLookupResult.Ambiguous(options)
        }

        val entries = byName.values.first()
        val pick = entries.firstOrNull { it.isMobile } ?: entries.first()
        Log.i(TAG, "Resolved '$query' to '${pick.name}'")
        return ContactLookupResult.Found(pick.name, pick.number)
    }

    fun resolvePhoneNumber(context: Context, input: String): String =
        when (val r = lookup(context, input)) {
            is ContactLookupResult.Found -> r.number
            else -> input.trim()
        }


    private fun preferredNumber(entries: List<Entry>): String =
        (entries.firstOrNull { it.isMobile } ?: entries.first()).number

    private fun loadContacts(context: Context): List<Entry> {
        val projection = arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME, Phone.TYPE)
        val result = mutableListOf<Entry>()
        context.contentResolver.query(Phone.CONTENT_URI, projection, null, null, null)?.use { c ->
            val numIdx = c.getColumnIndexOrThrow(Phone.NUMBER)
            val nameIdx = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
            val typeIdx = c.getColumnIndexOrThrow(Phone.TYPE)
            while (c.moveToNext()) {
                val name = c.getString(nameIdx) ?: continue
                val number = c.getString(numIdx) ?: continue
                result += Entry(name, tokenize(name), number, c.getInt(typeIdx) == Phone.TYPE_MOBILE)
            }
        }
        if (USE_TEST_CONTACTS) {
            TEST_CONTACTS.forEach { (name, number) ->
                result += Entry(name, tokenize(name), number, true)
            }
        }

        return result
    }

    private fun score(query: List<String>, entry: Entry): Int? {
        var total = 0
        for (q in query) {
            val best = entry.tokens.maxOfOrNull { tokenScore(q, it) } ?: 0
            if (best == 0) return null
            total += best
        }
        val extra = (entry.tokens.size - query.size).coerceAtLeast(0)
        return total * 10 - extra
    }

    private fun tokenScore(q: String, c: String): Int = when {
        q == c -> 4
        stemVariants(q).intersect(stemVariants(c)).isNotEmpty() -> 3
        else -> 0
    }

    private fun fold(s: String): String {
        val lower = s.lowercase(POLISH).replace('ł', 'l')
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }

    private fun tokenize(s: String): List<String> =
        fold(s).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    private fun stemVariants(t: String): Set<String> {
        val stripped = buildSet {
            add(t)
            for (end in ENDINGS) {
                if (t.endsWith(end) && t.length - end.length >= 2) add(t.removeSuffix(end))
            }
        }
        return buildSet {
            addAll(stripped)
            for (s in stripped) {
                if (s.endsWith("iek")) add(s.dropLast(3) + "k")
                if (s.endsWith("ek")) add(s.dropLast(2) + "k")
                if (s.endsWith("iec")) add(s.dropLast(3) + "c")
                if (s.endsWith("ec")) add(s.dropLast(2) + "c")
                if (s.endsWith("el")) add(s.dropLast(2) + "l")
                if (s.endsWith("er")) add(s.dropLast(2) + "r")
            }
        }
    }
}