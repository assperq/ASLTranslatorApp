package com.example.handtranslator.translator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun InputModeSelector(inputMode: InputMode, onInputModeChange: (InputMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = inputMode == InputMode.CAMERA,
            onClick = { onInputModeChange(InputMode.CAMERA) },
            label = { Text("Ввод с камеры") }
        )
        FilterChip(
            selected = inputMode == InputMode.TEXT,
            onClick = { onInputModeChange(InputMode.TEXT) },
            label = { Text("Текстовый ввод") }
        )
    }
}