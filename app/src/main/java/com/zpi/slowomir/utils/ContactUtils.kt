package com.zpi.slowomir.utils

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object ContactUtils {

    private const val TAG = "ContactUtils"

    /**
     * Attempts to resolve a given name or raw text string into a phone number by querying
     * Android's Contacts ContentProvider.
     *
     * @param context The application context.
     * @param input The display name or phone number string to search.
     * @return The resolved phone number string, or the original [input] if no contact match was found.
     */
    fun resolvePhoneNumber(context: Context, input: String): String {
        val rawTrimmed = input.trim()
        if (rawTrimmed.isBlank()) return ""

        // Remove Polish prepositions if present at the start of input
        val trimmed = rawTrimmed.removePrefix("do ")
            .removePrefix("z ")
            .trim()

        // If input consists only of phone number digits/symbols, return as-is
        if (trimmed.all { (it.isDigit()) || (it == '+') || (it == ' ') || (it == '-') || (it == '(') || (it == ')') }) {
            return trimmed
        }

        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$trimmed%")

        return try {
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        val number = cursor.getString(numberIndex)
                        Log.i(TAG, "Resolved contact '$trimmed' to phone number '$number'")
                        return@use number
                    }
                }
                Log.w(TAG, "No contact found matching name '$trimmed'")
                trimmed
            } ?: trimmed
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact '$trimmed'", e)
            trimmed
        }
    }
}
