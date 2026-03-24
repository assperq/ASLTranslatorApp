package com.example.handtranslator.translator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R

@Composable
fun InputModeSelector(inputMode: InputMode, onInputModeChange: (InputMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilterChip(
            selected = inputMode == InputMode.CAMERA,
            onClick = { onInputModeChange(InputMode.CAMERA) },
            label = { Text(stringResource(R.string.mode_camera)) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = inputMode == InputMode.TEXT,
            onClick = { onInputModeChange(InputMode.TEXT) },
            label = { Text(stringResource(R.string.mode_text)) },
            modifier = Modifier.weight(1f)
        )
    }
}
