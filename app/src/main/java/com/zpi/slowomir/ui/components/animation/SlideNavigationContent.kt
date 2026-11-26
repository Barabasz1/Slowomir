package com.zpi.slowomir.ui.components.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

enum class NavigationDirection { Forward, Backward }

/**
 * Slides complete destinations within a fixed viewport.
 * Include destination-specific bars in [content] to avoid resizing during a transition.
 * Navigation and savable state remain the caller's responsibility.
 *
 * @param targetState The destination to slide to.
 * @param direction Determines the direction of the slide.
 * @param modifier Modifier to be applied to the layout.
 * @param label Used to track and debug Compose animations.
 * @param content The content to slide.
 */
@Composable
fun <T> SlideNavigationContent(
    targetState: T,
    direction: (initialState: T, targetState: T) -> NavigationDirection,
    modifier: Modifier = Modifier,
    label: String = "SlideNavigation",
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            val slideDirection = when (direction(initialState, targetState)) {
                NavigationDirection.Forward -> AnimatedContentTransitionScope.SlideDirection.Left
                NavigationDirection.Backward -> AnimatedContentTransitionScope.SlideDirection.Right
            }
            ((slideIntoContainer(slideDirection, tween(300)) + fadeIn(tween(300))) togetherWith
                (slideOutOfContainer(slideDirection, tween(300)) + fadeOut(tween(300))))
                .using(null)
        },
        label = label,
    ) { state -> content(state) }
}
