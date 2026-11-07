package com.zpi.slowomir.overlay.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zpi.slowomir.overlay.ui.common.PickerOptionItem
import com.zpi.slowomir.overlay.ui.common.SelectionPickerCard
import com.zpi.slowomir.utils.ContactOption

@Composable
fun ContactPickerView(
    options: List<ContactOption>,
    onSelect: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = options.mapIndexed { i, option ->
        PickerOptionItem(
            position = i + 1,
            title = option.name,
            subtitle = option.number,
            onClick = { onSelect(i + 1) }
        )
    }

    SelectionPickerCard(
        headerText = "Słucham…",
        title = "Do kogo zadzwonić?",
        instruction = "Powiedz numer z listy, np. „dwa”",
        items = items,
        onCancel = onCancel,
        modifier = modifier
    )
}
