package com.zpi.slowomir.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zpi.slowomir.ui.components.animation.NavigationDirection
import com.zpi.slowomir.ui.components.animation.SlideNavigationContent

private enum class OnboardingStep {
    WELCOME,
    PERMISSIONS
}

@Composable
fun OnboardingFlow(
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }

    BackHandler(enabled = step == OnboardingStep.PERMISSIONS) {
        step = OnboardingStep.WELCOME
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        SlideNavigationContent(
            targetState = step,
            direction = { _, target ->
                if (target == OnboardingStep.PERMISSIONS) {
                    NavigationDirection.Forward
                } else {
                    NavigationDirection.Backward
                }
            },
            label = "onboardingTransition",
            modifier = Modifier.fillMaxSize()
        ) { currentStep ->
            when (currentStep) {
                OnboardingStep.WELCOME -> OnboardingWelcomeScreen(
                    onStartClick = { step = OnboardingStep.PERMISSIONS }
                )
                OnboardingStep.PERMISSIONS -> OnboardingPermissionsScreen(
                    onFinishClick = onOnboardingFinished
                )
            }
        }
    }
}
