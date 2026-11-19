package com.zpi.slowomir.overlay.ui.voice

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.voice.mediator.CommandMediator
import com.zpi.slowomir.ui.icons.edit
import com.zpi.slowomir.ui.icons.mic
import com.zpi.slowomir.ui.icons.speech_to_text
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.stt.TextMode
import com.zpi.slowomir.voice.stt.VoiceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal enum class Kind { LISTENING, DICTATING, EDITING, PROCESSING, SUCCESS, ERROR }

internal data class Content(val text: String, val kind: Kind)

internal fun VoiceState.toSessionContent(): Content? = when (this) {
    is VoiceState.Dictating -> Content(partialText, Kind.DICTATING)
    is VoiceState.Editing -> Content(partialText, Kind.EDITING)
    is VoiceState.TextProcessing -> when (mode) {
        TextMode.DICTATE -> Content(rawText, Kind.DICTATING)
        TextMode.EDIT -> Content(rawText, Kind.EDITING)
    }

    else -> null
}

internal fun chooseContent(session: Content?, overlay: Content?, result: Content?): Content? = when {
    session == null -> overlay
    session.text.isNotBlank() -> session
    else -> result ?: session
}

internal fun OverlayState.toContent(): Content? = when (this) {
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
private const val MIN_DISPLAY_MS = 1_000L

internal fun <T> Flow<T>.withMinDisplayTime(
    minMs: Long,
    sameKind: (T, T) -> Boolean,
): Flow<T> = flow {
    var previous: T? = null
    var hasPrevious = false
    var shownAt = 0L
    buffer(Channel.UNLIMITED).collect { next ->
        if (hasPrevious && !sameKind(previous as T, next)) {
            val remaining = minMs - (System.nanoTime() - shownAt) / 1_000_000
            if (remaining > 0) delay(remaining)
            shownAt = System.nanoTime()
        } else if (!hasPrevious) {
            shownAt = System.nanoTime()
        }
        previous = next
        hasPrevious = true
        emit(next)
    }
}

@Composable
fun VoiceOverlayView(mediator: CommandMediator, voiceState: StateFlow<VoiceState>) {
    val uiState by mediator.mediatorState.collectAsState()
    val state by voiceState.collectAsState()
    var sessionResult by remember { mutableStateOf<Content?>(null) }

    val session = state.toSessionContent()
    val overlayContent = uiState.overlayState.toContent()

    LaunchedEffect(uiState.overlayState) {
        val content = uiState.overlayState.toContent()
        if (content?.kind == Kind.SUCCESS || content?.kind == Kind.ERROR) {
            sessionResult = content
            delay(RESULT_VISIBLE_MS.milliseconds)
            sessionResult = null
        }
    }

    val target = chooseContent(session, overlayContent, sessionResult)
    val targetState = rememberUpdatedState(target)
    val current by remember {
        snapshotFlow { targetState.value }
            .withMinDisplayTime(MIN_DISPLAY_MS) { a, b -> a?.kind == b?.kind }
    }.collectAsState(initial = null)

    var shown by remember { mutableStateOf<Content?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(current) {
        val content = current
        if (content == null) {
            visible = false
            return@LaunchedEffect
        }
        shown = content
        visible = true
        when (content.kind) {
            Kind.SUCCESS, Kind.ERROR -> {
                delay(RESULT_VISIBLE_MS.milliseconds); visible = false
            }

            Kind.LISTENING, Kind.PROCESSING, Kind.DICTATING, Kind.EDITING -> {
                delay(SILENCE_TIMEOUT_MS.milliseconds); visible = false
            }
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface,
                spotColor = MaterialTheme.colorScheme.onSurface
            )
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
            StatusIcon(content.kind)
            if (content.text.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = content.text,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}


@Composable
private fun kindColor(kind: Kind): Color = when (kind) {
    Kind.LISTENING, Kind.DICTATING -> MaterialTheme.colorScheme.primary
    Kind.EDITING ->  Color(0xFFFFA000)
    Kind.PROCESSING -> MaterialTheme.colorScheme.tertiary
    Kind.SUCCESS -> MaterialTheme.colorScheme.secondary
    Kind.ERROR -> MaterialTheme.colorScheme.error
}

private fun Kind.icon(): ImageVector = when (this) {
    Kind.LISTENING -> mic
    Kind.DICTATING -> speech_to_text
    Kind.EDITING -> edit
    Kind.PROCESSING -> Icons.Filled.Refresh
    Kind.SUCCESS -> Icons.Filled.Check
    Kind.ERROR -> Icons.Filled.Warning
}

@Composable
private fun StatusIcon(kind: Kind) {
    val target = kindColor(kind)
    val color by animateColorAsState(target, tween(250), label = "iconColor")

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val active =
        kind == Kind.LISTENING || kind == Kind.PROCESSING || kind == Kind.DICTATING || kind == Kind.EDITING

    Icon(
        imageVector = kind.icon(),
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(22.dp)
            .alpha(if (active) pulseAlpha else 1f),
    )
}

