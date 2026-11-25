package com.zpi.slowomir.ui.components.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Gives click feedback before invoking the action; repeated clicks are ignored while animating.
 *
 * @param onClick The action to be performed when the button is clicked.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, the button will not be clickable and will appear disabled to accessibility services.
 * @param content The button content.
 */
@Composable
fun BounceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    var animating by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            if (!animating && enabled) {
                animating = true
                scope.launch {
                    try {
                        scale.animateTo(0.85f, tween(40))
                        scale.animateTo(1f, tween(40))
                        if (currentEnabled) currentOnClick()
                    } finally {
                        animating = false
                    }
                }
            }
        },
        enabled = enabled && !animating,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        content = content,
    )
}
