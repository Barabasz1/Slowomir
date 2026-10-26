package com.zpi.slowomir.overlay.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.overlay.mediator.CommandMediator
import com.zpi.slowomir.overlay.voice.OverlayState

/**
 * Floating system overlay Composable that renders real-time speech transcription
 * and pipeline execution status directly from [CommandMediator.mediatorState].
 */
@Composable
fun VoiceOverlayView(mediator: CommandMediator) {
    val uiState by mediator.mediatorState.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = when (val state = uiState.overlayState) {
                    is OverlayState.ProcessingCommand -> state.text
                    is OverlayState.ListeningToSpeech -> state.partialText
                    else -> "Listening..."
                },
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )

        }
    }
}
