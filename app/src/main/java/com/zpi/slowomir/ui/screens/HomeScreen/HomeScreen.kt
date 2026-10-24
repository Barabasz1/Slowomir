package com.zpi.slowomir.ui.screens.HomeScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.voice.VoiceUiState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val resultState by viewModel.uiState.collectAsState()
    val voiceUiState by viewModel.voiceUiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    val sendCommand = {
        if (inputText.isNotBlank()) {
            viewModel.simulateVoiceInput(inputText)
            inputText = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tap Command Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.executeTapAt(x = 500f, y = 500f) }
        ) {
            Text("Execute Tap at (10f, 10f)")
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Wpisz komendę (np. 'Kliknij 5')") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { sendCommand() })
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { sendCommand() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wyślij polecenie")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Stan Mowy: ${renderVoiceState(voiceUiState)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Render command result
        when (val result = resultState) {
            is CommandResult.Success -> {
                Text(
                    text = "Status: Tap Executed Successfully",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is CommandResult.Failure -> {
                Text(
                    text = "Status: Execution Failed - ${result.reason}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            is CommandResult.InvalidContext -> {
                Text(
                    text = "Status: Invalid Context - ${result.reason}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            null -> {
                Text(text = "Status: Idle")
            }
        }
    }
}

private fun renderVoiceState(state: VoiceUiState): String {
    return when (state) {
        is VoiceUiState.Idle -> "Idle"
        is VoiceUiState.ListeningForWakeWord -> "Listening for wake-word..."
        is VoiceUiState.ListeningToSpeech -> "Listening: '${state.partialText}'"
        is VoiceUiState.ProcessingCommand -> "Processing: '${state.text}'"
        is VoiceUiState.CommandExecuted -> "Executed: ${state.resultName}"
        is VoiceUiState.Error -> "Error: ${state.message}"
    }
}
