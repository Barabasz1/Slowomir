package com.zpi.slowomir.overlay.ui.macro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zpi.slowomir.overlay.macro.Macro
import com.zpi.slowomir.ui.components.animation.OverlayAppearContent
import com.zpi.slowomir.ui.components.animation.rememberPulse

@Composable
fun MacroView(
    macroList: List<MacroEntry>,
    modifier: Modifier = Modifier,
    onSelectMacro: (Int) -> Unit = {},
    onClose: () -> Unit = {},
) {
    OverlayAppearContent(
        modifier = modifier.fillMaxSize(),
        enterDurationMillis = MacroView.FADE_IN_DURATION_MS,
        exitDurationMillis = MacroView.FADE_OUT_DURATION_MS,
        initialOffsetY = MacroView.SLIDE_IN_OFFSET_Y,
        initialScale = MacroView.INITIAL_SCALE,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MacroView.OVERLAY_PADDING)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(MacroView.CARD_CORNER_RADIUS),
                elevation = CardDefaults.cardElevation(defaultElevation = MacroView.CARD_ELEVATION),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MacroView.CONTENT_PADDING_HORIZONTAL,
                                vertical = MacroView.HEADER_PADDING_VERTICAL
                            ),
                        verticalArrangement = Arrangement.spacedBy(MacroView.HEADER_TEXT_SPACING)
                    ) {
                        MacroHeaderIndicator()
                        MacroTitleSection()
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (macroList.isEmpty()) {
                            EmptyMacroState()
                        } else {
                            MacroListContent(
                                macroList = macroList,
                                onSelectMacro = onSelectMacro
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MacroView.CONTENT_PADDING_HORIZONTAL,
                                vertical = MacroView.FOOTER_PADDING_VERTICAL
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        CloseButton(onClose = onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroHeaderIndicator() {
    val scale by rememberPulse(
        initialValue = 0.8f,
        targetValue = 1.2f,
        durationMillis = MacroView.PULSE_ANIMATION_DURATION_MS,
        label = "macro_pulse",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MacroView.HEADER_INDICATOR_GAP),
    ) {
        Box(
            modifier = Modifier
                .size(MacroView.INDICATOR_DOT_SIZE)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(
            text = "LIST MAKR",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MacroTitleSection() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Twoje makra",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Wywołaj makro, wypowiadając jego numer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroListContent(
    macroList: List<MacroEntry>,
    onSelectMacro: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MacroView.LIST_VERTICAL_PADDING)
    ) {
        itemsIndexed(macroList) { index, entry ->
            MacroEntryRowHorizontal(
                position = index + 1,
                entry = entry,
                onClick = { onSelectMacro(index + 1) }
            )

            if (index < macroList.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = MacroView.ROW_PADDING_HORIZONTAL)
                )
            }
        }
    }
}

@Composable
private fun MacroEntryRowHorizontal(
    position: Int,
    entry: MacroEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MacroView.ROW_PADDING_HORIZONTAL,
                vertical = MacroView.ROW_PADDING_VERTICAL
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MacroView.ITEM_CONTENT_SPACING),
    ) {
        IndexBadge(
            position = position,
            isGenerated = entry.isGenerated
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (entry.list.isNotEmpty()) {
                ActionSequenceViewHorizontal(actions = entry.list)
            } else {
                Text(
                    text = "Brak akcji",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Suppress("unused")
@Composable
private fun MacroEntryRowVertical(
    position: Int,
    entry: MacroEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MacroView.ROW_PADDING_HORIZONTAL,
                vertical = MacroView.ROW_PADDING_VERTICAL
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MacroView.ITEM_CONTENT_SPACING),
    ) {
        IndexBadge(
            position = position,
            isGenerated = entry.isGenerated
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (entry.list.isNotEmpty()) {
                ActionSequenceViewVertical(actions = entry.list)
            } else {
                Text(
                    text = "Brak akcji",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IndexBadge(
    position: Int,
    isGenerated: Boolean
) {
    val containerColor = if (isGenerated) {
        MacroView.COLOR_GENERATED_CONTAINER
    } else {
        MacroView.COLOR_RECORDED_CONTAINER
    }

    val textColor = if (isGenerated) {
        MacroView.COLOR_GENERATED_TEXT
    } else {
        MacroView.COLOR_RECORDED_TEXT
    }

    Box(
        modifier = Modifier
            .size(MacroView.INDEX_CIRCLE_SIZE)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Composable
private fun ActionSequenceViewHorizontal(actions: List<ActionEntry>) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(MacroView.ACTION_CHIP_VERTICAL_SPACING, Alignment.CenterVertically),
        horizontalArrangement = Arrangement.spacedBy(MacroView.ACTION_CHIP_SPACING)
    ) {
        actions.forEachIndexed { index, action ->
            if (index > 0) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            ActionChip(text = action.text)
        }
    }
}

@Composable
private fun ActionSequenceViewVertical(actions: List<ActionEntry>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MacroView.ACTION_CHIP_VERTICAL_SPACING)
    ) {
        actions.forEachIndexed { index, action ->
            ActionChip(text = action.text)
        }
    }
}

@Composable
private fun ActionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(MacroView.ACTION_CHIP_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = MacroView.ACTION_CHIP_PADDING_HORIZONTAL,
                vertical = MacroView.ACTION_CHIP_PADDING_VERTICAL
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyMacroState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = MacroView.EMPTY_STATE_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Brak zapisanych makr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Nagraj nowe makro za pomocą komendy głosowej.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClose,
        modifier = modifier,
    ) {
        Text(
            text = "Zamknij",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

object MacroView {
    // Layout & Card Dimensions
    val OVERLAY_PADDING: Dp = 12.dp
    val CARD_CORNER_RADIUS: Dp = 20.dp
    val CARD_ELEVATION: Dp = 8.dp
    val CONTENT_PADDING_HORIZONTAL: Dp = 16.dp
    val HEADER_PADDING_VERTICAL: Dp = 12.dp
    val FOOTER_PADDING_VERTICAL: Dp = 6.dp

    // Row & Item Dimensions
    val INDEX_CIRCLE_SIZE: Dp = 28.dp
    val ROW_PADDING_HORIZONTAL: Dp = 12.dp
    val ROW_PADDING_VERTICAL: Dp = 6.dp
    val LIST_VERTICAL_PADDING: Dp = 2.dp
    val INDICATOR_DOT_SIZE: Dp = 8.dp

    // Action Chips
    val ACTION_CHIP_CORNER_RADIUS: Dp = 5.dp
    val ACTION_CHIP_PADDING_HORIZONTAL: Dp = 6.dp
    val ACTION_CHIP_PADDING_VERTICAL: Dp = 2.dp
    val ACTION_CHIP_SPACING: Dp = 4.dp
    val ACTION_CHIP_VERTICAL_SPACING: Dp = 3.dp

    // Colors for Generated
    val COLOR_GENERATED_CONTAINER: Color = Color(0xFFFF9800)
    val COLOR_GENERATED_TEXT: Color = Color.White

    // Colors for Recorded
    val COLOR_RECORDED_CONTAINER: Color = Color(0xFF18AD6A)
    val COLOR_RECORDED_TEXT: Color = Color.White

    // Spacing
    val HEADER_TEXT_SPACING: Dp = 4.dp
    val HEADER_INDICATOR_GAP: Dp = 6.dp
    val ITEM_CONTENT_SPACING: Dp = 8.dp
    val EMPTY_STATE_PADDING: Dp = 16.dp

    // Animation Configurations
    const val FADE_IN_DURATION_MS: Int = 350
    const val FADE_OUT_DURATION_MS: Int = 250
    const val SLIDE_IN_OFFSET_Y: Int = 40
    const val INITIAL_SCALE: Float = 0.92f
    const val PULSE_ANIMATION_DURATION_MS: Int = 800

    // Window Layout Params
    const val WIDTH_FRACTION: Float = 1.0f
    const val HEIGHT_FRACTION: Float = 0.7f
}