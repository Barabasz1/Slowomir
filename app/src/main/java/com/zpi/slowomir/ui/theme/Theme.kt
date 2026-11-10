package com.zpi.slowomir.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SlowomirTeal = Color(0xFF2DD4C8)
val SlowomirBlue = Color(0xFF1F7BF0)

private val SlowomirNavy = Color(0xFF081020)

private val SlowomirDarkColors = darkColorScheme(
    primary = SlowomirTeal,
    onPrimary = Color(0xFF04101C),
    primaryContainer = Color(0xFF0F3A4A),
    onPrimaryContainer = Color(0xFF9FF0EA),

    secondary = Color(0xFF3B8BFF),
    onSecondary = Color(0xFF04101C),
    secondaryContainer = Color(0xFF12356B),
    onSecondaryContainer = Color(0xFFCFE2FF),

    tertiary = Color(0xFF5CC8F5),
    onTertiary = Color(0xFF04101C),
    tertiaryContainer = Color(0xFF0E3550),
    onTertiaryContainer = Color(0xFFC7ECFF),

    background = SlowomirNavy,
    onBackground = Color(0xFFE6EDF8),
    surface = SlowomirNavy,
    onSurface = Color(0xFFE6EDF8),
    surfaceVariant = Color(0xFF16233A),
    onSurfaceVariant = Color(0xFF9FB0C8),

    surfaceContainerLowest = Color(0xFF050B16),
    surfaceContainerLow = Color(0xFF0C1628),
    surfaceContainer = Color(0xFF111D33),
    surfaceContainerHigh = Color(0xFF16243D),
    surfaceContainerHighest = Color(0xFF1C2C48),

    outline = Color(0xFF5C6F8C),
    outlineVariant = Color(0xFF263650),

    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0A0A),
    errorContainer = Color(0xFF5A1F28),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun SłowomirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SlowomirDarkColors,
        content = content,
    )
}