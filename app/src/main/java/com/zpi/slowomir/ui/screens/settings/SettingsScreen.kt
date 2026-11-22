package com.zpi.slowomir.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zpi.slowomir.overlay.ui.grid.GridType
import com.zpi.slowomir.ui.screens.permissions.ManagePermissionsScreen
import com.zpi.slowomir.utils.PreferencesManager

private object Idx {
    const val PERMISSIONS = 1
    const val OVERLAY = 2
    const val INVERTED_CONTROLS = 3
    const val ROWS_MINUS = 4
    const val ROWS_PLUS = 5
    const val COLS_MINUS = 6
    const val COLS_PLUS = 7
    const val GRID_TYPE_FIRST = 8 // 9..10
}

@Composable
fun SettingsScreen(
    isOverlayEnabled: Boolean,
    onOverlayEnabledChange: (Boolean) -> Unit,
    gridRows: Int,
    onGridRowsChange: (Int) -> Unit,
    gridCols: Int,
    onGridColsChange: (Int) -> Unit,
    gridType: GridType,
    onGridTypeChange: (GridType) -> Unit,
    invertedControls: Boolean,
    onInvertedControlsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPermissionsScreen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showPermissionsScreen) {
        showPermissionsScreen = false
    }

    if (showPermissionsScreen) {
        ManagePermissionsScreen(
            onBack = { showPermissionsScreen = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ustawienia",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )

        SectionTitle("Ogólne")

        SettingsCard {
            val description = "Opcja ${Idx.PERMISSIONS}: Zarządzaj uprawnieniami"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = { showPermissionsScreen = true }
                    )
                    .clearAndSetSemantics {
                        contentDescription = description
                        role = Role.Button
                        onClick(label = null) { showPermissionsScreen = true; true }
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndexBadge(Idx.PERMISSIONS)
                Spacer(modifier = Modifier.width(12.dp))
                IconTile(Icons.Rounded.Lock)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Zarządzaj uprawnieniami",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isOverlayEnabled,
                        role = Role.Switch,
                        onValueChange = onOverlayEnabledChange
                    )
                    .clearAndSetSemantics {
                        contentDescription = "Opcja ${Idx.OVERLAY}: Pokaż nakładkę głosową"
                        stateDescription = if (isOverlayEnabled) "Włączona" else "Wyłączona"
                        role = Role.Switch
                        onClick(label = null) { onOverlayEnabledChange(!isOverlayEnabled); true }
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndexBadge(Idx.OVERLAY)
                Spacer(modifier = Modifier.width(12.dp))
                IconTile(Icons.Filled.Build)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Pokaż nakładkę głosową",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))

                Switch(checked = isOverlayEnabled, onCheckedChange = null)
            }
        }

        SectionTitle("Dotyk")

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = invertedControls,
                        role = Role.Switch,
                        onValueChange = onInvertedControlsChange
                    )
                    .clearAndSetSemantics {
                        contentDescription = "Opcja ${Idx.OVERLAY}: odwrócone sterowanie"
                        stateDescription = if (isOverlayEnabled) "Włączona" else "Wyłączona"
                        role = Role.Switch
                        onClick(label = null) { onOverlayEnabledChange(!isOverlayEnabled); true }
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndexBadge(Idx.INVERTED_CONTROLS)
                Spacer(modifier = Modifier.width(12.dp))
                IconTile(Icons.Filled.Build)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Odwrócone sterowanie",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))

                Switch(checked = invertedControls, onCheckedChange = null)
            }
        }

        SectionTitle("Siatka")

        SettingsCard {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Stepper(
                    title = "Liczba wierszy",
                    value = gridRows,
                    minusIndex = Idx.ROWS_MINUS,
                    plusIndex = Idx.ROWS_PLUS,
                    onValueChange = onGridRowsChange
                )
                Stepper(
                    title = "Liczba kolumn",
                    value = gridCols,
                    minusIndex = Idx.COLS_MINUS,
                    plusIndex = Idx.COLS_PLUS,
                    onValueChange = onGridColsChange
                )
            }
        }

        SettingsCard {
            Column(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .selectableGroup()
            ) {
                Text(
                    text = "Typ siatki",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .semantics { heading() }
                )
                GridType.entries.forEachIndexed { i, type ->
                    ChoiceRow(
                        index = Idx.GRID_TYPE_FIRST + i,
                        label = type.label(),
                        selected = type == gridType,
                        onSelect = { onGridTypeChange(type) },
                        trailing = { GridTypePreview(type) }
                    )
                }
            }
        }
    }
}

private fun GridType.label(): String = when (this) {
    GridType.SINGLE -> "Jednokolorowa"
    GridType.DASHED -> "Przerywana"
    GridType.CONTRAST -> "Kontrastowa"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        content()
    }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun IndexBadge(index: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ChoiceRow(
    index: Int,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics {
                contentDescription = "Opcja $index: $label"
                stateDescription = if (selected) "Wybrana" else "Niewybrana"
                role = Role.RadioButton
                onClick(label = null) { onSelect(); true }
            }
            .padding(horizontal = 18.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IndexBadge(index)
        Spacer(modifier = Modifier.width(12.dp))
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun Stepper(
    title: String,
    value: Int,
    minusIndex: Int,
    plusIndex: Int,
    onValueChange: (Int) -> Unit
) {
    val min = PreferencesManager.GRID_MIN
    val max = PreferencesManager.GRID_MAX

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Od $min do $max",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StepperButton(
            index = minusIndex,
            icon = Icons.Filled.Delete,
            description = "Opcja $minusIndex: zmniejsz – $title, obecnie $value",
            enabled = value > min,
            onClick = { onValueChange(value - 1) }
        )

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(44.dp)
                .semantics { contentDescription = "$title: $value" },
            textAlign = TextAlign.Center
        )

        StepperButton(
            index = plusIndex,
            icon = Icons.Filled.Add,
            description = "Opcja $plusIndex: zwiększ – $title, obecnie $value",
            enabled = value < max,
            onClick = { onValueChange(value + 1) }
        )
    }
}

@Composable
private fun StepperButton(
    index: Int,
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IndexBadge(index)
        Spacer(modifier = Modifier.height(4.dp))
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(56.dp)
                .semantics { contentDescription = description }
        ) {
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}

@Composable
private fun GridTypePreview(type: GridType) {
    val background = Color(0xFF3A3A3A)
    Canvas(
        modifier = Modifier
            .size(width = 64.dp, height = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .semantics { contentDescription = "" }
    ) {
        val strokePx = 2.dp.toPx()
        val vertical = listOf(size.width / 3f, size.width * 2f / 3f)
        val horizontal = listOf(size.height / 2f)

        fun line(from: Offset, to: Offset) {
            when (type) {
                GridType.SINGLE -> drawLine(Color.Yellow, from, to, strokePx)
                GridType.DASHED -> drawLine(
                    Color.Yellow, from, to, strokePx,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
                )

                GridType.CONTRAST -> {
                    drawLine(Color.Black, from, to, strokePx * 3f)
                    drawLine(Color.White, from, to, strokePx)
                }
            }
        }

        vertical.forEach { x -> line(Offset(x, 0f), Offset(x, size.height)) }
        horizontal.forEach { y -> line(Offset(0f, y), Offset(size.width, y)) }
    }
}
