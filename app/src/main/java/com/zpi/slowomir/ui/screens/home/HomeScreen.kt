package com.zpi.slowomir.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.ui.theme.SlowomirBlue
import com.zpi.slowomir.ui.theme.SlowomirTeal
import com.zpi.slowomir.voice.OverlayState
import com.zpi.slowomir.voice.stt.VoiceState
import kotlinx.coroutines.delay

enum class SlowomirState(val title: String) {
    Off("SłowoMIR wyłączony"),
    WaitingForWakeWord("SłowoMIR czeka na wybudzenie"),
    Listening("SłowoMIR nasłuchuje komend"),
    Processing("SłowoMIR przetwarza komendę"),
    Error("SłowoMIR napotkał błąd"),
    CommandExecuted("SłowoMIR wykonał komendę");

    val isRunning: Boolean
        get() = this != Off

    val isPulsing: Boolean
        get() = this == WaitingForWakeWord ||
                this == Listening ||
                this == Processing
}

fun computeSlowomirState(
    isServiceRunning: Boolean,
    voiceState: VoiceState,
    overlayState: OverlayState,
    resultState: CommandResult?,
    includeTransientResults: Boolean = true
): SlowomirState {
    if (!isServiceRunning) return SlowomirState.Off

    if (includeTransientResults) {
        if (voiceState is VoiceState.Failed ||
            overlayState is OverlayState.Error ||
            resultState is CommandResult.Failure ||
            resultState is CommandResult.InvalidContext
        ) {
            return SlowomirState.Error
        }

        if (overlayState is OverlayState.CommandExecuted || resultState is CommandResult.Success) {
            return SlowomirState.CommandExecuted
        }
    }

    if (voiceState is VoiceState.IntentParsing ||
        voiceState is VoiceState.CommandExecuting ||
        overlayState is OverlayState.ProcessingCommand
    ) {
        return SlowomirState.Processing
    }

    if (voiceState is VoiceState.Listening ||
        overlayState is OverlayState.ListeningForCommand ||
        overlayState is OverlayState.ListeningToSpeech
    ) {
        return SlowomirState.Listening
    }

    return SlowomirState.WaitingForWakeWord
}

private val SuccessGreen = Color(0xFF3DDC84)

@Composable
private fun SlowomirState.accentColor(): Color = when (this) {
    SlowomirState.Off -> MaterialTheme.colorScheme.outline
    SlowomirState.WaitingForWakeWord -> MaterialTheme.colorScheme.secondary
    SlowomirState.Listening -> MaterialTheme.colorScheme.primary
    SlowomirState.Processing -> MaterialTheme.colorScheme.tertiary
    SlowomirState.Error -> MaterialTheme.colorScheme.error
    SlowomirState.CommandExecuted -> SuccessGreen
}


@Composable
fun HomeScreen(
    isServiceRunning: Boolean,
    voiceState: VoiceState,
    overlayState: OverlayState,
    resultState: CommandResult?,
    onStartVoiceService: () -> Unit,
    onStopVoiceService: () -> Unit,
    modifier: Modifier = Modifier,
    onIntroductionClick: () -> Unit = {},
    onCommandsClick: () -> Unit = {},
    showIntroHighlight: Boolean = false,
    onDismissIntroHighlight: () -> Unit = {},
) {
    val context = LocalContext.current
    var showTransientResult by remember { mutableStateOf(false) }
    var isFirstRun by remember { mutableStateOf(true) }

    LaunchedEffect(overlayState, resultState) {
        if (isFirstRun) {
            isFirstRun = false
            return@LaunchedEffect
        }
        showTransientResult = true
        delay(3000L)
        showTransientResult = false
    }

    val state = remember(isServiceRunning, voiceState, overlayState, resultState, showTransientResult) {
        computeSlowomirState(
            isServiceRunning = isServiceRunning,
            voiceState = voiceState,
            overlayState = overlayState,
            resultState = resultState,
            includeTransientResults = showTransientResult
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartVoiceService()
        }
    }

    val toggleVoiceService = {
        if (isServiceRunning) {
            onStopVoiceService()
        } else {
            val hasRecordAudio = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasRecordAudio) {
                onStartVoiceService()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppTitle()
        }

        Spacer(Modifier.height(24.dp))

        StatusPanel(
            state = state,
            onToggleVoiceService = toggleVoiceService,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            if (showIntroHighlight) {
                IntroTooltipBanner(
                    onDismiss = onDismissIntroHighlight,
                )
            }

            NavTile(
                title = "Instrukcja",
                icon = Icons.Rounded.Info,
                isHighlighted = showIntroHighlight,
                onClick = {
                    if (showIntroHighlight) {
                        onDismissIntroHighlight()
                    }
                    onIntroductionClick()
                },
            )

            Spacer(Modifier.height(24.dp))

            NavTile(
                title = "Lista komend",
                icon = Icons.AutoMirrored.Rounded.List,
                onClick = onCommandsClick,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}


@Composable
private fun AppTitle() {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            ) {
                append("Słowo")
            }

            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(
                        listOf(SlowomirTeal, SlowomirBlue)
                    ),
                )
            ) {
                append("MIR")
            }
        },
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 44.sp,
            letterSpacing = 1.sp,
        ),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatusPanel(
    state: SlowomirState,
    onToggleVoiceService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent by animateColorAsState(
        targetValue = state.accentColor(),
        animationSpec = tween(400),
        label = "statusAccent",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 24.dp, vertical = 36.dp),
    ) {
        Text(
            text = "AKTUALNY STAN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = accent,
        )

        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(250)) +
                        slideInVertically(tween(250)) { it / 4 }) togetherWith
                        (fadeOut(tween(150)) +
                                slideOutVertically(tween(150)) { -it / 4 })
            },
            label = "statusText",
        ) { current ->
            Text(
                text = current.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }

        Spacer(Modifier.height(32.dp))

        StartStopButton(
            isRunning = state.isRunning,
            onClick = onToggleVoiceService,
        )
    }
}

@Composable
private fun StartStopButton(
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    val stopColor = MaterialTheme.colorScheme.errorContainer

    val startColor by animateColorAsState(
        targetValue = if (isRunning) stopColor else SlowomirTeal,
        animationSpec = tween(300),
        label = "buttonStart",
    )

    val endColor by animateColorAsState(
        targetValue = if (isRunning) stopColor else SlowomirBlue,
        animationSpec = tween(300),
        label = "buttonEnd",
    )

    val contentColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(300),
        label = "buttonContent",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(startColor, endColor)
                )
            )
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "buttonLabel",
        ) { running ->
            Text(
                text = if (running) {
                    "Zatrzymaj nasłuchiwanie"
                } else {
                    "Rozpocznij nasłuchiwanie"
                },
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}



@Composable
private fun IntroTooltipBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 6.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "Zobacz, jak korzystać z aplikacji",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Zamknij podpowiedź",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}




@Composable
private fun NavTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
) {
    val highlightColor = Color(0xFFFF9800)

    val borderColor = if (isHighlighted) {
        highlightColor
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = if (isHighlighted) 2.dp else 1.dp

    val containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = if (isHighlighted) 0.dp else 24.dp,
            topEnd = if (isHighlighted) 0.dp else 24.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        border = BorderStroke(
            borderWidth,
            borderColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isHighlighted) {
                            highlightColor.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlighted) {
                        highlightColor
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }

            Spacer(Modifier.size(16.dp))

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
