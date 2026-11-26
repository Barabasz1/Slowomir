package com.zpi.slowomir.ui.navigation

import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zpi.slowomir.ui.screens.permissions.ManagePermissionsScreen
import androidx.compose.ui.text.style.TextAlign
import com.zpi.slowomir.BuildConfig
import com.zpi.slowomir.ui.components.animation.NavigationDirection
import com.zpi.slowomir.ui.components.animation.SlideNavigationContent
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
    var showPermissionsScreen by rememberSaveable { mutableStateOf(false) }

    val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
    val showIntroHighlight by mainViewModel.showIntroHighlight.collectAsState()
    val isServiceRunning by mainViewModel.isVoiceRunning.collectAsState()
    val voiceState by mainViewModel.voiceState.collectAsState()
    val mediatorState by mainViewModel.overlayState.collectAsState()
    val resultState by mainViewModel.uiState.collectAsState()
    val isOverlayEnabled by mainViewModel.isOverlayEnabled.collectAsState()
    val gridRows by mainViewModel.gridRows.collectAsState()
    val gridCols by mainViewModel.gridCols.collectAsState()
    val gridType by mainViewModel.gridType.collectAsState()
    val invertedControls by mainViewModel.invertedControls.collectAsState()
    val recordedMacros by mainViewModel.recordedMacros.collectAsState()
    val generatedMacros by mainViewModel.generatedMacros.collectAsState()

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

    val visibleTabs = AppTab.entries.filter { tab ->
        tab != AppTab.DEV || BuildConfig.DEBUG
    }

    val isManagingPermissions = selectedTab == AppTab.SETTINGS && showPermissionsScreen
    val mainContentState = rememberSaveableStateHolder()

    BackHandler(enabled = isManagingPermissions) {
        showPermissionsScreen = false
    }

    // Animate the entire destination, including its navigation bar. Removing the bar
    // from a shared Scaffold would resize both screens in the middle of the slide.
    SlideNavigationContent(
        targetState = isManagingPermissions,
        direction = { _, target ->
            if (target) NavigationDirection.Forward else NavigationDirection.Backward
        },
        label = "Settings to Permissions Animation",
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { managingPermissions ->
        if (managingPermissions) {
            ManagePermissionsScreen(
                onBack = { showPermissionsScreen = false },
                modifier = Modifier.fillMaxSize().safeDrawingPadding()
            )
        } else {
            // Restore the settings scroll position after returning from permissions.
            mainContentState.SaveableStateProvider("main") {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            visibleTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        if (tab == AppTab.HOME && selectedTab == AppTab.HOME) {
                                            homeDestination = null
                                        }
                                        selectedTab = tab
                                    },
                                    icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)

                    when (selectedTab) {
                        AppTab.HOME -> {
                            SlideNavigationContent(
                                targetState = homeDestination,
                                direction = { _, target ->
                                    if (target != null) NavigationDirection.Forward else NavigationDirection.Backward
                                },
                                label = "Home Navigation Animation",
                                modifier = contentModifier.fillMaxSize()
                            ) { targetDestination ->
                                when (targetDestination) {
                                    null -> HomeScreen(
                                        isServiceRunning = isServiceRunning,
                                        voiceState = voiceState,
                                        overlayState = mediatorState.overlayState,
                                        resultState = resultState,
                                        onStartVoiceService = { mainViewModel.startVoiceService() },
                                        onStopVoiceService = { mainViewModel.stopVoiceService() },
                                        modifier = Modifier,
                                        onIntroductionClick = { homeDestination = HomeDestination.INTRODUCTION },
                                        onCommandsClick = { homeDestination = HomeDestination.COMMANDS },
                                        showIntroHighlight = showIntroHighlight,
                                        onDismissIntroHighlight = { mainViewModel.dismissIntroHighlight() },
                                    )

                                    HomeDestination.INTRODUCTION -> IntroductionScreen(
                                        onBack = { homeDestination = null },
                                        modifier = Modifier,
                                    )

                                    HomeDestination.COMMANDS -> CommandsScreen(
                                        onBack = { homeDestination = null },
                                        modifier = Modifier,
                                    )
                                }
                            }
                        }

            AppTab.MACROS -> MacrosScreen(
                macros = recordedMacros,
                generatedMacros = generatedMacros,
                onKeepMacro = { mainViewModel.keepMacro(it) },
                onDeleteMacro = { mainViewModel.deleteMacro(it) },
                onScreenVisibleChange = { mainViewModel.setMacrosScreenVisible(it) },
                modifier = contentModifier,
            )
            AppTab.SETTINGS -> SettingsScreen(
                isOverlayEnabled = isOverlayEnabled,
                onOverlayEnabledChange = { mainViewModel.setOverlayEnabled(it) },
                gridRows = gridRows,
                onGridRowsChange = { mainViewModel.setGridRows(it) },
                gridCols = gridCols,
                onGridColsChange = { mainViewModel.setGridCols(it) },
                gridType = gridType,
                onGridTypeChange = { mainViewModel.setGridType(it) },
                invertedControls = invertedControls,
                onInvertedControlsChange = { mainViewModel.setInvertedControls(it) },
                onManagePermissionsClick = { showPermissionsScreen = true },
                modifier = contentModifier
            )

                        AppTab.DEV -> if (BuildConfig.DEBUG) {
                            DevScreen(
                                onSpeechInput = { mainViewModel.simulateSpeechInput(it) },
                                onSpeechCompleted = { mainViewModel.simulateSpeechCompletion(it) },
                                onWakeWord = { mainViewModel.simulateWakeWord() },
                                onToggleGrid = { mainViewModel.toggleGrid() },
                                onToggleLabels = { mainViewModel.toggleLabels() },
                                onTapAt = { x, y -> mainViewModel.executeTapAt(x, y) },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
