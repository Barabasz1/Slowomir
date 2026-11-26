package com.zpi.slowomir.ui.screens.macros

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.zpi.slowomir.overlay.ui.macro.ActionSequenceViewHorizontal
import com.zpi.slowomir.overlay.ui.macro.IndexBadge
import com.zpi.slowomir.overlay.ui.macro.MacroEntry
import com.zpi.slowomir.overlay.ui.macro.MacroSectionHeader

@Composable
fun MacrosScreen(
    macros: List<MacroEntry>,
    generatedMacros: List<MacroEntry>,
    onKeepMacro: (position: Int) -> Unit,
    onDeleteMacro: (position: Int) -> Unit,
    onScreenVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportScreenVisibility(onScreenVisibleChange)

    var pendingDelete by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Makra",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Najpierw Twoje nagrania (usuń: kosz lub „usuń makro N”), potem makra wyliczone z Twoich zwyczajów (zachowaj: przycisk lub „zachowaj N”).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (macros.isEmpty() && generatedMacros.isEmpty()) {
            EmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (macros.isNotEmpty()) item { MacroSectionHeader("Zapisane") }
                itemsIndexed(macros) { index, entry ->
                    MacroRow(
                        position = index + 1,
                        entry = entry,
                        isGenerated = false,
                        onAction = { pendingDelete = index + 1 }
                    )
                }
                if (generatedMacros.isNotEmpty()) item { MacroSectionHeader("Proponowane") }
                itemsIndexed(generatedMacros) { index, entry ->
                    val position = macros.size + index + 1
                    MacroRow(
                        position = position,
                        entry = entry,
                        isGenerated = true,
                        onAction = { onKeepMacro(position) }
                    )
                }
            }
        }
    }

    pendingDelete?.let { position ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Usunąć makro $position?") },
            text = { Text("Tej operacji nie można cofnąć.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDeleteMacro(position)
                }) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Anuluj") }
            }
        )
    }
}

@Composable
private fun MacroRow(
    position: Int,
    entry: MacroEntry,
    isGenerated: Boolean,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IndexBadge(position = position, isGenerated = isGenerated)

            Box(modifier = Modifier.weight(1f)) {
                ActionSequenceViewHorizontal(actions = entry.list)
            }

            if (isGenerated) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.semantics { contentDescription = "Zachowaj makro $position" }
                ) {
                    Text("Zachowaj")
                }
            } else {
                IconButton(
                    onClick = onAction,
                    modifier = Modifier.semantics { contentDescription = "Usuń makro $position" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Brak nagranych makr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Powiedz „nagraj makro”, aby nagrać pierwsze.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun ReportScreenVisibility(onChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycle = remember(context) { context.findLifecycleOwner()?.lifecycle }

    DisposableEffect(lifecycle) {
        if (lifecycle == null) {
            onChange(true)
            return@DisposableEffect onDispose { onChange(false) }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onChange(true)
                Lifecycle.Event.ON_PAUSE -> onChange(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            onChange(false)
        }
    }
}

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is LifecycleOwner) return current
        current = current.baseContext
    }
    return null
}
