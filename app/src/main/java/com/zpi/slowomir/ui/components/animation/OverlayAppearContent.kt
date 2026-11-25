package com.zpi.slowomir.ui.components.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Plays the overlay entrance once each time the component enters composition.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param enterDurationMillis Duration of the enter animation in milliseconds.
 * @param exitDurationMillis Duration of the exit animation in milliseconds.
 * @param initialOffsetY Initial offset of the overlay in pixels.
 * @param initialScale Initial scale of the overlay.
 * @param content The content to be displayed.
 */
@Composable
fun OverlayAppearContent(
    modifier: Modifier = Modifier,
    enterDurationMillis: Int = 300,
    exitDurationMillis: Int = 200,
    initialOffsetY: Int = 40,
    initialScale: Float = 0.9f,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    AnimatedVisibility(
        visible = appeared,
        modifier = modifier,
        enter = fadeIn(tween(enterDurationMillis)) +
            slideInVertically(tween(enterDurationMillis)) { initialOffsetY } +
            scaleIn(tween(enterDurationMillis), initialScale),
        exit = fadeOut(tween(exitDurationMillis)),
        content = content,
    )
}
