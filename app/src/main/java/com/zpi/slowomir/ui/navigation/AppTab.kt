package com.zpi.slowomir.ui.navigation

import com.zpi.slowomir.R
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    HOME(R.string.tab_home, Icons.Filled.Home),
    MACROS(R.string.tab_macros, Icons.Filled.PlayArrow),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
    DEV(R.string.tab_dev, Icons.Filled.Build)
}