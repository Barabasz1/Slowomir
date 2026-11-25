package com.zpi.slowomir.ui.components.animation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Shared reversing pulse for scale, opacity, and other visual indicators.
 *
 * @param initialValue The initial value of the animation.
 * @param targetValue The target value of the animation.
 * @param durationMillis The duration of the animation in milliseconds.
 * @param label A label for the animation.
 * @param easing The easing function to use for the animation.
 * @return A [State] object containing the current value of the animation.
 */
@Composable
fun rememberPulse(
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int,
    label: String,
    easing: Easing = FastOutSlowInEasing,
): State<Float> {
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = easing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "${label}Value",
    )
}
