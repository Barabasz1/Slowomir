package com.zpi.slowomir.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zpi.slowomir.ui.screens.dev.DevScreen
import com.zpi.slowomir.ui.screens.viewmodel.MainViewModel
import com.zpi.slowomir.ui.screens.home.CommandsScreen
import com.zpi.slowomir.ui.screens.home.HomeScreen
import com.zpi.slowomir.ui.screens.home.IntroductionScreen
import com.zpi.slowomir.ui.screens.macros.MacrosScreen
import com.zpi.slowomir.ui.screens.onboarding.OnboardingFlow
import com.zpi.slowomir.ui.screens.settings.SettingsScreen

private enum class HomeDestination {
    INTRODUCTION,
    COMMANDS
}

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var homeDestination by rememberSaveable { mutableStateOf<HomeDestination?>(null) }

    val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
    val showIntroHighlight by mainViewModel.showIntroHighlight.collectAsState()
    val isServiceRunning by mainViewModel.isVoiceRunning.collectAsState()
    val voiceState by mainViewModel.voiceState.collectAsState()
    val mediatorState by mainViewModel.overlayState.collectAsState()
    val resultState by mainViewModel.uiState.collectAsState()
    val isOverlayEnabled by mainViewModel.isOverlayEnabled.collectAsState()

    if (!isOnboardingCompleted) {
        OnboardingFlow(
            onOnboardingFinished = { mainViewModel.completeOnboarding() },
            modifier = modifier
        )
        return
    }

    BackHandler(enabled = selectedTab == AppTab.HOME && homeDestination != null) {
        homeDestination = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == AppTab.HOME && selectedTab == AppTab.HOME) {
                                homeDestination = null
                            }
                            selectedTab = tab
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (selectedTab) {
            AppTab.HOME -> when (homeDestination) {
                null -> HomeScreen(
                    isServiceRunning = isServiceRunning,
                    voiceState = voiceState,
                    overlayState = mediatorState.overlayState,
                    resultState = resultState,
                    onStartVoiceService = { mainViewModel.startVoiceService() },
                    onStopVoiceService = { mainViewModel.stopVoiceService() },
                    modifier = contentModifier,
                    onIntroductionClick = { homeDestination = HomeDestination.INTRODUCTION },
                    onCommandsClick = { homeDestination = HomeDestination.COMMANDS },
                    showIntroHighlight = showIntroHighlight,
                    onDismissIntroHighlight = { mainViewModel.dismissIntroHighlight() },
                )

                HomeDestination.INTRODUCTION -> IntroductionScreen(
                    onBack = { homeDestination = null },
                    modifier = contentModifier,
                )

                HomeDestination.COMMANDS -> CommandsScreen(
                    onBack = { homeDestination = null },
                    modifier = contentModifier,
                )
            }

            AppTab.MACROS -> MacrosScreen(modifier = contentModifier)
            AppTab.SETTINGS -> SettingsScreen(
                isOverlayEnabled = isOverlayEnabled,
                onOverlayEnabledChange = { mainViewModel.setOverlayEnabled(it) },
                modifier = contentModifier
            )

            AppTab.DEV -> DevScreen(
                onSpeechInput = { mainViewModel.simulateSpeechInput(it) },
                onSpeechCompleted = { mainViewModel.simulateSpeechCompletion(it) },
                onWakeWord = { mainViewModel.simulateWakeWord() },
                onToggleGrid = { mainViewModel.toggleGrid() },
                onTapAt = { x, y -> mainViewModel.executeTapAt(x, y) },
                modifier = contentModifier
            )
        }
    }
}
