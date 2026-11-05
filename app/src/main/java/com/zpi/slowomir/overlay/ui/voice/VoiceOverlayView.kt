package com.zpi.slowomir.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.voice.OverlayState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private enum class Kind { LISTENING, PROCESSING, SUCCESS, ERROR }
private data class Content(val text: String, val kind: Kind)

private fun OverlayState.toContent(): Content? = when (this) {
    is OverlayState.Idle,
    is OverlayState.ListeningForKeyword -> null

    is OverlayState.ListeningForCommand ->
        Content("Listening…", Kind.LISTENING)

    is OverlayState.ListeningToSpeech ->
        Content(partialText.ifBlank { "Listening…" }, Kind.LISTENING)

    is OverlayState.ProcessingCommand -> Content(text, Kind.PROCESSING)
    is OverlayState.CommandExecuted -> Content(resultName, Kind.SUCCESS)
    is OverlayState.Error -> Content(message, Kind.ERROR)
}

private const val RESULT_VISIBLE_MS = 5_000L
private const val SILENCE_TIMEOUT_MS = 8_000L

@Composable
fun VoiceOverlayView(mediator: CommandMediator) {
    val uiState by mediator.mediatorState.collectAsState()
    val current = uiState.overlayState.toContent()

    var shown by remember { mutableStateOf<Content?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        if (current == null) {
            visible = false
            return@LaunchedEffect
        }
        shown = current
        visible = true
        when (current.kind) {
            Kind.SUCCESS, Kind.ERROR -> {
                delay(RESULT_VISIBLE_MS.milliseconds); visible = false
            }

            Kind.LISTENING -> {
                delay(SILENCE_TIMEOUT_MS.milliseconds); visible = false
            }

            Kind.PROCESSING -> Unit
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) +
                slideInVertically(tween(250)) { -it / 2 } +
                scaleIn(tween(250), initialScale = 0.92f),
        exit = fadeOut(tween(200)) +
                slideOutVertically(tween(200)) { -it / 2 } +
                scaleOut(tween(200), targetScale = 0.92f),
    ) {
        shown?.let { OverlayCard(it) }
    }
}

@Composable
private fun OverlayCard(content: Content) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface,
                spotColor = MaterialTheme.colorScheme.onSurface
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(animationSpec = tween(durationMillis = 80, easing = LinearEasing)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(content.kind)
            Spacer(Modifier.width(12.dp))
            Text(
                text = content.text,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun StatusDot(kind: Kind) {
    val target = when (kind) {
        Kind.LISTENING -> MaterialTheme.colorScheme.primary
        Kind.PROCESSING -> MaterialTheme.colorScheme.tertiary
        Kind.SUCCESS -> MaterialTheme.colorScheme.secondary
        Kind.ERROR -> MaterialTheme.colorScheme.error
    }
    val color by animateColorAsState(target, tween(250), label = "dotColor")

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val active = kind == Kind.LISTENING || kind == Kind.PROCESSING

    Box(
        Modifier
            .size(10.dp)
            .alpha(if (active) pulseAlpha else 1f)
            .background(color, CircleShape)
    )
}

