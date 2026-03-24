package com.example.handtranslator.translator

import android.content.res.Configuration
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.handtranslator.R
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

@Composable
fun MainScreen(
    inputMode: InputMode,
    onInputModeChange: (InputMode) -> Unit,
    cameraContentMode: CameraContentMode,
    selectedMediaUri: Uri?,
    selectedMediaType: SelectedMediaType,
    showLandmarks: Boolean,
    onShowLandmarksChange: (Boolean) -> Unit,
    cameraFacing: CameraFacing,
    onCameraFacingChange: (CameraFacing) -> Unit,
    isTorchSupported: Boolean,
    isTorchEnabled: Boolean,
    onTorchEnabledChange: (Boolean) -> Unit,
    recognizedText: List<Letter>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    landmarks: List<NormalizedLandmark>,
    onPreviewViewReady: (PreviewView) -> Unit,
    onClearRecognizedText: (Boolean) -> Unit,
    onSelectMedia: (Uri) -> Unit,
    onSwitchToCameraPreview: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderBar()
            InputModeSelector(inputMode = inputMode, onInputModeChange = onInputModeChange)

            val contentModifier = Modifier.weight(1f)
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = contentModifier.fillMaxWidth()) {
                    MainContent(
                        inputMode,
                        cameraContentMode,
                        selectedMediaUri,
                        selectedMediaType,
                        showLandmarks,
                        onShowLandmarksChange,
                        cameraFacing,
                        onCameraFacingChange,
                        isTorchSupported,
                        isTorchEnabled,
                        onTorchEnabledChange,
                        recognizedText,
                        textInput,
                        onTextInputChange,
                        landmarks,
                        onPreviewViewReady,
                        onClearRecognizedText,
                        onSelectMedia,
                        onSwitchToCameraPreview,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                MainContent(
                    inputMode,
                    cameraContentMode,
                    selectedMediaUri,
                    selectedMediaType,
                    showLandmarks,
                    onShowLandmarksChange,
                    cameraFacing,
                    onCameraFacingChange,
                    isTorchSupported,
                    isTorchEnabled,
                    onTorchEnabledChange,
                    recognizedText,
                    textInput,
                    onTextInputChange,
                    landmarks,
                    onPreviewViewReady,
                    onClearRecognizedText,
                    onSelectMedia,
                    onSwitchToCameraPreview,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun HeaderBar() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("HandTranslator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.source_language) + " → " + stringResource(R.string.target_language),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun MainContent(
    inputMode: InputMode,
    cameraContentMode: CameraContentMode,
    selectedMediaUri: Uri?,
    selectedMediaType: SelectedMediaType,
    showLandmarks: Boolean,
    onShowLandmarksChange: (Boolean) -> Unit,
    cameraFacing: CameraFacing,
    onCameraFacingChange: (CameraFacing) -> Unit,
    isTorchSupported: Boolean,
    isTorchEnabled: Boolean,
    onTorchEnabledChange: (Boolean) -> Unit,
    recognizedText: List<Letter>,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    landmarks: List<NormalizedLandmark>,
    onPreviewViewReady: (PreviewView) -> Unit,
    onClearRecognizedText: (Boolean) -> Unit,
    onSelectMedia: (Uri) -> Unit,
    onSwitchToCameraPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.weight(1f),
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (inputMode == InputMode.CAMERA) {
                if (cameraContentMode == CameraContentMode.LIVE_CAMERA) {
                    CameraPanel(
                        showLandmarks = showLandmarks,
                        onShowLandmarksChange = onShowLandmarksChange,
                        cameraFacing = cameraFacing,
                        onCameraFacingChange = onCameraFacingChange,
                        landmarks = if (showLandmarks) landmarks else emptyList(),
                        onPreviewViewReady = onPreviewViewReady,
                        isTorchSupported = isTorchSupported,
                        isTorchEnabled = isTorchEnabled,
                        onTorchEnabledChange = onTorchEnabledChange,
                        onSelectMedia = onSelectMedia
                    )
                } else {
                    MediaPanel(
                        selectedMediaUri = selectedMediaUri,
                        selectedMediaType = selectedMediaType,
                        onSelectMedia = onSelectMedia,
                        onSwitchToCameraPreview = onSwitchToCameraPreview
                    )
                }
            } else {
                TextInputPanel(textInput = textInput, onTextInputChange = onTextInputChange)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            TranslationPanel(recognizedText, onClearRecognizedText)
        }
    }
}
