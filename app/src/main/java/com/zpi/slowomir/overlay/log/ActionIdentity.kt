package com.zpi.slowomir.overlay.log

import java.security.MessageDigest

object HierarchyPath {
    const val MAX_SEGMENTS = 8
    const val SEPARATOR = "/"

    fun segment(className: CharSequence?, indexInParent: Int): String {
        val simpleName = className
            ?.toString()
            ?.substringAfterLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown"
        return "$simpleName[$indexInParent]"
    }

    fun format(segments: List<String>): String =
        segments.takeLast(MAX_SEGMENTS).joinToString(SEPARATOR)
}

object ActionHasher {
    private const val SEPARATOR = "\u001F"
    const val NULL_MARKER = "\u0000"

    fun hash(action: ActionDescriptor): String {
        val isClick = action.type == ActionType.UI_CLICK
        val parts = listOf(
            action.type.name,
            (if (isClick) action.packageName else null) ?: NULL_MARKER,
            (if (isClick) action.resourceId else null) ?: NULL_MARKER,
            (if (isClick) action.hierarchyPath else null) ?: NULL_MARKER,
            (if (isClick) action.labelHash else null) ?: NULL_MARKER,
        )
        return sha256Hex(parts.joinToString(SEPARATOR))
    }

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}