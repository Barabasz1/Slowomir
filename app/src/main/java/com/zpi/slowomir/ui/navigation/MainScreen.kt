package com.zpi.slowomir.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zpi.slowomir.ui.screens.dev.DevScreen
import com.zpi.slowomir.ui.screens.dev.DevViewModel
import com.zpi.slowomir.ui.screens.home.HomeScreen
import com.zpi.slowomir.ui.screens.macros.MacrosScreen
import com.zpi.slowomir.ui.screens.settings.SettingsScreen


@Composable
fun MainScreen(
    devViewModel: DevViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            AppTab.HOME -> HomeScreen(modifier = contentModifier)
            AppTab.MACROS -> MacrosScreen(modifier = contentModifier)
            AppTab.SETTINGS -> SettingsScreen(modifier = contentModifier)
            AppTab.DEV -> DevScreen(viewModel = devViewModel, modifier = contentModifier)
        }
    }
}