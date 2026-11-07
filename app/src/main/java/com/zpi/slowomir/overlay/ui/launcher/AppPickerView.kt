package com.zpi.slowomir.overlay.ui.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zpi.slowomir.overlay.executors.AppItem
import com.zpi.slowomir.overlay.ui.common.PickerOptionItem
import com.zpi.slowomir.overlay.ui.common.SelectionPickerCard

@Composable
fun AppPickerView(
    options: List<AppItem>,
    onSelect: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = options.mapIndexed { i, app ->
        PickerOptionItem(
            position = i + 1,
            title = app.name,
            subtitle = app.packageName,
            icon = {
                val bitmap = app.icon.toBitmap(48, 48).asImageBitmap()
                Image(
                    bitmap = bitmap,
                    contentDescription = app.name,
                    modifier = Modifier.size(28.dp)
                )
            },
            onClick = { onSelect(i + 1) }
        )
    }

    SelectionPickerCard(
        headerText = "Wybierz aplikację…",
        title = "Którą aplikację otworzyć?",
        instruction = "Powiedz numer z listy lub dotknij aplikację",
        items = items,
        onCancel = onCancel,
        modifier = modifier
    )
}
