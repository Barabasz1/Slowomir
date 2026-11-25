package com.zpi.slowomir.ui.components.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Animated content with fade-in and fade-out animations.
 *
 * @param targetState The target state of the content.
 * @param modifier Modifier to be applied to the content.
 * @param label A label for the animation.
 * @param content The content to be animated.
 */
@Composable
fun <T> FadeContent(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "FadeContent",
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = label,
    ) { state -> content(state) }
}

@Composable
fun <T> SlideFadeContent(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "SlideFadeContent",
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 4 }) togetherWith
                (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 4 })
        },
        label = label,
    ) { state -> content(state) }
}
