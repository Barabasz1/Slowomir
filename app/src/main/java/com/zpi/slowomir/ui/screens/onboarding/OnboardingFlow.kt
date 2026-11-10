package com.zpi.slowomir.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

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
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState == OnboardingStep.PERMISSIONS) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
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
