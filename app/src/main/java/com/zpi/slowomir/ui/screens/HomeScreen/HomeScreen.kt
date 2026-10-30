package com.zpi.slowomir.ui.screens.HomeScreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.voice.OverlayState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val resultState by viewModel.uiState.collectAsState()
    val mediatorState by viewModel.overlayState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val isServiceRunning by viewModel.isVoiceServiceRunning.collectAsState()
    val liveSpeechState by viewModel.liveSpeechState.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceService(context)
        }
    }

    val toggleVoiceService = {
        if (isServiceRunning) {
            viewModel.stopVoiceService(context)
        } else {
            val hasRecordAudio = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasRecordAudio) {
                viewModel.startVoiceService(context)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    val sendCommand = {
        if (inputText.isNotBlank()) {
            viewModel.simulateSpeechCompletion(inputText)
            inputText = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),

        ) {
        Text(
            text = "Command Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                viewModel.simulateSpeechInput(it)
            },
            label = { Text("Enter a command (i.e. 'Kliknij 5')") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { sendCommand() })
        )

        Button(
            onClick = { sendCommand() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Command")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Voice Status: ${renderVoiceStatus(mediatorState.overlayState, isServiceRunning)}",
            style = MaterialTheme.typography.bodyLarge
        )


        // Command Execution Status
        when (val result = resultState) {
            is CommandResult.Success -> {
                Text(
                    text = "Last Action: Executed successfully",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is CommandResult.Failure -> {
                Text(
                    text = "Last Action: Failed - ${result.reason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is CommandResult.InvalidContext -> {
                Text(
                    text = "Last Action: Invalid Context - ${result.reason}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            null -> {
                Text(
                    text = "Last Action: None",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = toggleVoiceService,
            colors = if (isServiceRunning) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isServiceRunning) "Stop Voice Recognition" else "Start Voice Recognition")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Recognized live text (Vosk):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (liveSpeechState.text.isBlank()) {
                    Text(
                        text = liveSpeechState.statusMessage ?: (if (isServiceRunning) "Waiting for voice..." else "Voice service is stopped"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = liveSpeechState.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (liveSpeechState.isPartial) FontWeight.Normal else FontWeight.Bold,
                        fontStyle = if (liveSpeechState.isPartial) FontStyle.Italic else FontStyle.Normal,
                        color = if (liveSpeechState.isPartial) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }


        Button(
            onClick = { viewModel.toggleGrid() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Turn On Grid")
        }

        Button(
            onClick = { viewModel.toggleVoiceOverlay() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toggle Overlay")
        }

        Button(
            onClick = { viewModel.executeTapAt(x = 500f, y = 500f) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Execute Tap at (500f, 500f)")
        }

        Button(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Display Over Other Apps")
        }

        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable Accessibility Service")
        }
    }
}

private fun renderVoiceStatus(state: OverlayState, isServiceRunning: Boolean): String {
    if (!isServiceRunning && state is OverlayState.Idle) {
        return "Stopped"
    }
    return when (state) {
        is OverlayState.Idle -> if (isServiceRunning) "Listening..." else "Stopped"
        is OverlayState.ListeningForWakeWord -> "Listening for wake-word..."
        is OverlayState.ListeningToSpeech -> "Listening..."
        is OverlayState.ProcessingCommand -> "Processing..."
        is OverlayState.CommandExecuted -> "Executed: ${state.resultName}"
        is OverlayState.Error -> "Error: ${state.message}"
    }
}

