package com.zpi.slowomir.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zpi.slowomir.R
import com.zpi.slowomir.ui.components.animation.BounceIconButton

/**
 * Button for navigating back. This is a wrapper around [BounceIconButton].
 *
 * @param onClick Called when the button is clicked.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param contentDescription Text used by accessibility services to describe what this button does.
 */
@Composable
fun BackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = stringResource(R.string.action_back),
) {
    BounceIconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
        )
    }
}
