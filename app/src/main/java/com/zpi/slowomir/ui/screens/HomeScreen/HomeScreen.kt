package com.zpi.slowomir.ui.screens.HomeScreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
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
import com.zpi.slowomir.voice.stt.VoiceState

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
    val isServiceRunning by viewModel.isVoiceRunning.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()

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

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCallPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAnswerCallsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasContactsPermission = isGranted }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCallPermission = isGranted }

    val answerCallsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasAnswerCallsPermission = isGranted }

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

        Button(
            onClick = { viewModel.simulateWakeWord() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate Wake Word")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Voice Status: ${
                renderVoiceStatus(
                    mediatorState.overlayState,
                    isServiceRunning
                )
            }",
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

            is CommandResult.NeedsSelection -> {
                Text(
                    text = "Last Action: Waiting for contact selection",
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

                when (val state = voiceState) {
                    is VoiceState.Listening -> Text(
                        text = state.partialText.ifBlank { "Waiting for voice command..." },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (state.partialText.isNotBlank()) FontWeight.Normal else FontWeight.Light,
                        fontStyle = if (state.partialText.isNotBlank()) FontStyle.Italic else FontStyle.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    is VoiceState.IntentParsing -> Text(
                        text = state.rawText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    is VoiceState.CommandExecuting -> Text(
                        text = "Executing command...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    is VoiceState.Failed -> Text(
                        text = "Error: ${state.reason}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )

                    VoiceState.KeywordSpotting,
                    VoiceState.Stopped -> Text(
                        text = renderVoiceStatus(state),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Show Voice Overlay",
                )

                Switch(
                    checked = isOverlayEnabled,
                    onCheckedChange = { viewModel.setOverlayEnabled(it) }
                )
            }
        }

        Button(
            onClick = { viewModel.toggleGrid() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Turn On Grid")
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

        Button(
            onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
            enabled = !hasContactsPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasContactsPermission) "Read Contacts: Granted" else "Enable Read Contacts")
        }

        Button(
            onClick = { callPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
            enabled = !hasCallPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasCallPermission) "Call Phone: Granted" else "Enable Call Phone")
        }

        Button(
            onClick = { answerCallsPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS) },
            enabled = !hasAnswerCallsPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasAnswerCallsPermission) "Answer/Decline Calls: Granted" else "Enable Answer/Decline Calls")
        }
    }
}

private fun renderVoiceStatus(state: VoiceState): String = when (state) {
    is VoiceState.Stopped -> "Voice service is stopped"
    is VoiceState.KeywordSpotting -> "Listening for wake-word ('Słowomir')..."
    is VoiceState.Listening -> "Waiting for voice command..."
    is VoiceState.IntentParsing -> "Parsing intent..."
    is VoiceState.CommandExecuting -> "Executing command..."
    is VoiceState.Failed -> "Failed"
}

private fun renderVoiceStatus(state: OverlayState, isServiceRunning: Boolean): String {
    if (!isServiceRunning) return "Stopped"
    return when (state) {
        is OverlayState.Idle -> "Idle"
        is OverlayState.ListeningForKeyword -> "Listening for wake-word ('Słowomir')..."
        is OverlayState.ListeningForCommand -> "Listening for command..."
        is OverlayState.ListeningToSpeech -> "Listening to speech..."
        is OverlayState.ProcessingCommand -> "Processing..."
        is OverlayState.CommandExecuted -> "Executed: ${state.resultName}"
        is OverlayState.Error -> "Error: ${state.message}"
    }
}

