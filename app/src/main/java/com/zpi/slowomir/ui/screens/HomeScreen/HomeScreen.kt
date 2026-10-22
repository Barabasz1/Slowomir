package com.zpi.slowomir.ui.screens.HomeScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.overlay.commands.CommandResult

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val resultState by viewModel.uiState.collectAsState()

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

        // Trigger valid tap command execution
        Button(
            onClick = { viewModel.executeTapAt(x = 500f, y = 500f) }
        ) {
            Text("Execute Tap at (10f, 10f)")
        }

        Spacer(modifier = Modifier.height(32.dp))

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