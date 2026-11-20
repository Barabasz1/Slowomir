package com.zpi.slowomir.prediction

import android.view.accessibility.AccessibilityNodeInfo
import java.text.Normalizer
import java.util.Locale

object PredictionGuard {

    private val BLOCKED_PACKAGES = setOf(
        "com.google.android.keep", // example or configurable
        // Banking & Wallets
        "pl.mbank",
        "pl.pkobp.iko",
        "com.ing.mojeing",
        "pl.santander.mobile",
        "pl.bph.mobile",
        "pl.pekao.pekao24",
        "com.revolut.revolut",
        "com.google.android.apps.walletnfcrel",
        // Password Managers
        "com.x8bit.bitwarden",
        "com.onepassword.android",
        "keepass2android.keepass2android",
        "com.lastpass.lpandroid",
        // Gov & ID
        "pl.nask.mobywatel",
    )

    private val PROTECTED_PREFIXES = listOf(
        "usu",
        "skas",
        "wykas",
        "wysl",
        "kup",
        "zaplac",
        "przelej",
        "zatwierdz",
        "formatuj",
        "zresetuj",
    )

    fun isGuardedPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        return BLOCKED_PACKAGES.any { packageName.startsWith(it, ignoreCase = true) }
    }

    fun isGuardedNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return true
        if (node.isPassword) return true
        if (node.isEditable) return true

        val candidates = listOfNotNull(node.text, node.contentDescription, node.hintText)
        for (c in candidates) {
            val normalized = normalizeText(c.toString())
            if (isProtectedLabel(normalized)) return true
        }
        return false
    }

    fun isProtectedLabel(normalizedText: String): Boolean {
        val words = normalizedText.split("\\s+".toRegex())
        for (word in words) {
            for (prefix in PROTECTED_PREFIXES) {
                if (word.startsWith(prefix)) return true
            }
        }
        return false
    }

    fun normalizeText(text: String): String {
        val nfd = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace('ł', 'l')
    }
}
