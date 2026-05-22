package com.example.handtranslator.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R

@Composable
fun PreferencesScreen(
    predictionCooldown: Long,
    requiredMatches: Int,
    frameSampleIntervalMs: Long,
    liveConfidenceThreshold: Float,
    photoConfidenceThreshold: Float,
    videoConfidenceThreshold: Float,
    videoFrameSampleIntervalMs: Long,
    videoPreviewFillEnabled: Boolean,
    onPredictionCooldownChange: (Long) -> Unit,
    onRequiredMatchesChange: (Int) -> Unit,
    onFrameSampleIntervalMsChange: (Long) -> Unit,
    onLiveConfidenceThresholdChange: (Float) -> Unit,
    onPhotoConfidenceThresholdChange: (Float) -> Unit,
    onVideoConfidenceThresholdChange: (Float) -> Unit,
    onVideoFrameSampleIntervalMsChange: (Long) -> Unit,
    onVideoPreviewFillEnabledChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsHeader(onBackClick = onBackClick)
            Text(stringResource(R.string.settings_live_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SettingsSliderCard(
                title = stringResource(R.string.settings_prediction_cooldown),
                value = stringResource(R.string.settings_ms_value, predictionCooldown),
                sliderValue = predictionCooldown.toFloat(),
                valueRange = 100f..1000f,
                steps = 17,
                onValueChange = { onPredictionCooldownChange(it.toLong()) }
            )

            SettingsSliderCard(
                title = stringResource(R.string.settings_required_matches),
                value = requiredMatches.toString(),
                sliderValue = requiredMatches.toFloat(),
                valueRange = 1f..6f,
                steps = 4,
                onValueChange = { onRequiredMatchesChange(it.toInt()) }
            )

            SettingsSliderCard(
                title = stringResource(R.string.settings_frame_interval),
                value = stringResource(R.string.settings_ms_value, frameSampleIntervalMs),
                sliderValue = frameSampleIntervalMs.toFloat(),
                valueRange = 50f..220f,
                steps = 16,
                onValueChange = { onFrameSampleIntervalMsChange(it.toLong()) }
            )

            SettingsSliderCard(
                title = stringResource(R.string.settings_confidence_threshold),
                value = stringResource(R.string.settings_percent_value, (liveConfidenceThreshold * 100).toInt()),
                sliderValue = liveConfidenceThreshold,
                valueRange = 0.1f..0.9f,
                steps = 15,
                onValueChange = { onLiveConfidenceThresholdChange((it * 100).toInt() / 100f) }
            )
            Text(stringResource(R.string.settings_media_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SettingsSliderCard(
                title = stringResource(R.string.settings_photo_confidence_threshold),
                value = stringResource(R.string.settings_percent_value, (photoConfidenceThreshold * 100).toInt()),
                sliderValue = photoConfidenceThreshold,
                valueRange = 0.1f..0.95f,
                steps = 16,
                onValueChange = { onPhotoConfidenceThresholdChange((it * 100).toInt() / 100f) }
            )
            SettingsSliderCard(
                title = stringResource(R.string.settings_video_confidence_threshold),
                value = stringResource(R.string.settings_percent_value, (videoConfidenceThreshold * 100).toInt()),
                sliderValue = videoConfidenceThreshold,
                valueRange = 0.05f..0.9f,
                steps = 16,
                onValueChange = { onVideoConfidenceThresholdChange((it * 100).toInt() / 100f) }
            )
            SettingsSliderCard(
                title = stringResource(R.string.settings_video_frame_interval),
                value = stringResource(R.string.settings_ms_value, videoFrameSampleIntervalMs),
                sliderValue = videoFrameSampleIntervalMs.toFloat(),
                valueRange = 300f..3000f,
                steps = 17,
                onValueChange = { onVideoFrameSampleIntervalMsChange(it.toLong()) }
            )
            SettingsToggleCard(
                title = stringResource(R.string.settings_video_resize_mode),
                subtitle = if (videoPreviewFillEnabled) stringResource(R.string.settings_video_fill) else stringResource(R.string.settings_video_fit),
                checked = videoPreviewFillEnabled,
                onCheckedChange = onVideoPreviewFillEnabledChange
            )
        }
    }
}

@Composable
private fun SettingsToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsHeader(
    onBackClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_main)
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SettingsSliderCard(
    title: String,
    value: String,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}
