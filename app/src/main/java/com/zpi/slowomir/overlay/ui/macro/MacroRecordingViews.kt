package com.zpi.slowomir.overlay.ui.macro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MacroRecordingSummary(actions: List<ActionEntry>, actionCount: Int = actions.size) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Nagrano ${actionCountText(actionCount)}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        actions.forEachIndexed { index, action ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MacroView.COLOR_RECORDED_CONTAINER,
                )
                Text(text = action.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

internal fun actionCountText(count: Int): String = when {
    count == 1 -> "1 akcja"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "$count akcje"
    else -> "$count akcji"
}
