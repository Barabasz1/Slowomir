package com.zpi.slowomir.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Strona główna", Icons.Filled.Home),
    MACROS("Makra", Icons.Filled.PlayArrow),
    SETTINGS("Ustawienia", Icons.Filled.Settings),
    DEV("Dev", Icons.Filled.Build)
}