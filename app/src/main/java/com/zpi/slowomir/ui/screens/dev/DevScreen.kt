package com.zpi.slowomir.ui.screens.dev

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun DevScreen(
    onSpeechInput: (String) -> Unit,
    onSpeechCompleted: (String) -> Unit,
    onWakeWord: () -> Unit,
    onToggleGrid: () -> Unit,
    onTapAt: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    val sendCommand = {
        if (inputText.isNotBlank()) {
            onSpeechCompleted(inputText)
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
        Text(text = "Dev", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                onSpeechInput(it)
            },
            label = { Text("Enter a command (i.e. 'Kliknij 5')") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { sendCommand() })
        )

        Button(onClick = { sendCommand() }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Command")
        }

        Button(onClick = onWakeWord, modifier = Modifier.fillMaxWidth()) {
            Text("Simulate Wake Word")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onToggleGrid, modifier = Modifier.fillMaxWidth()) {
            Text("Turn On Grid")
        }

        Button(onClick = { onTapAt(500f, 500f) }, modifier = Modifier.fillMaxWidth()) {
            Text("Execute Tap at (500f, 500f)")
        }
    }
}