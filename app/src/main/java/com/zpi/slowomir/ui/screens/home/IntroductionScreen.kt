package com.zpi.slowomir.ui.screens.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IntroductionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoScreenLayout(
        title = "Wprowadzenie do aplikacji",
        onBack = onBack,
        modifier = modifier,
    ) {
        repeat(3) { index ->
            if (index > 0) Spacer(Modifier.height(16.dp))
            Text(
                text = LOREM_IPSUM,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}