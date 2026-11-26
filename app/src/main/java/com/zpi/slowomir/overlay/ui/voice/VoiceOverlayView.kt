package com.zpi.slowomir.overlay.ui.voice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.ui.components.animation.rememberPulse
import com.zpi.slowomir.ui.icons.edit
import com.zpi.slowomir.ui.icons.hourglass_empty
import com.zpi.slowomir.ui.icons.mic
import com.zpi.slowomir.ui.icons.speech_to_text
import com.zpi.slowomir.voice.OverlayState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun VoiceOverlayView(state: StateFlow<OverlayState>, macroStatus: StateFlow<MacroStatus?>) {
    val current by state.collectAsState()
    val macro by macroStatus.collectAsState()
    if (!current.isHidden || macro != null) OverlayCard(current, macro)
}

@Composable
private fun OverlayCard(state: OverlayState, macro: MacroStatus?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, Color(0xFF3DDC84))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 80,
                        easing = LinearEasing
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!state.isHidden) {
                StatusIcon(state)
                val text = state.displayText()
                if (text.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = text,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            if (macro != null) {
                if (!state.isHidden) Spacer(Modifier.width(12.dp))
                MacroStatusChip(macro, showLabel = state.isHidden)
            }
        }
    }
}

@Composable
private fun OverlayState.color(): Color = when (this) {
    is OverlayState.Idle, is OverlayState.ListeningForKeyword,
    is OverlayState.ListeningForCommand, is OverlayState.ListeningToSpeech,
    is OverlayState.Dictating -> MaterialTheme.colorScheme.primary

    is OverlayState.Editing -> Color(0xFFFFA000)
    is OverlayState.ProcessingCommand -> MaterialTheme.colorScheme.tertiary
    is OverlayState.CommandExecuted -> MaterialTheme.colorScheme.secondary
    is OverlayState.Error -> MaterialTheme.colorScheme.error
}

private fun OverlayState.icon(): ImageVector = when (this) {
    is OverlayState.Idle, is OverlayState.ListeningForKeyword,
    is OverlayState.ListeningForCommand, is OverlayState.ListeningToSpeech -> mic

    is OverlayState.Dictating -> speech_to_text
    is OverlayState.Editing -> edit
    is OverlayState.ProcessingCommand -> hourglass_empty
    is OverlayState.CommandExecuted -> Icons.Filled.Check
    is OverlayState.Error -> Icons.Filled.Warning
}

@Composable
private fun StatusIcon(state: OverlayState) {
    val color by animateColorAsState(state.color(), tween(250), label = "iconColor")

    val pulseAlpha by rememberPulse(
        initialValue = 1f,
        targetValue = 0.3f,
        durationMillis = 700,
        label = "pulseAlpha",
    )

    Icon(
        imageVector = state.icon(),
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(22.dp)
            .alpha(if (state.isResult) 1f else pulseAlpha),
    )
}

private val RecordingRed = Color(0xFFE53935)

@Composable
private fun MacroStatusChip(status: MacroStatus, showLabel: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            is MacroStatus.Recording -> {
                val transition = rememberInfiniteTransition(label = "recordingBlink")
                val dotAlpha by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.2f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "recordingDotAlpha",
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(dotAlpha)
                        .clip(CircleShape)
                        .background(RecordingRed)
                )
                if (showLabel) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Zapisz makro",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = RecordingRed,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            is MacroStatus.Playing -> {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF18AD6A),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Makro: zostało ${status.remaining}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
