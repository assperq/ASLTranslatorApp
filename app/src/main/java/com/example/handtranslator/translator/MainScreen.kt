package com.example.handtranslator.translator

import android.content.res.Configuration
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark


@Composable
fun MainScreen(
    inputMode: InputMode,
    onInputModeChange: (InputMode) -> Unit,
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
    onSelectPhoto: (Uri) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            InputModeSelector(inputMode = inputMode, onInputModeChange = onInputModeChange)
            Spacer(modifier = Modifier.height(16.dp))

            when (LocalConfiguration.current.orientation) {

                Configuration.ORIENTATION_LANDSCAPE -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MainContent(
                            inputMode = inputMode,
                            showLandmarks = showLandmarks,
                            onShowLandmarksChange = onShowLandmarksChange,
                            cameraFacing = cameraFacing,
                            onCameraFacingChange = onCameraFacingChange,
                            isTorchSupported = isTorchSupported,
                            isTorchEnabled = isTorchEnabled,
                            onTorchEnabledChange = onTorchEnabledChange,
                            recognizedText = recognizedText,
                            textInput = textInput,
                            onTextInputChange = onTextInputChange,
                            landmarks = landmarks,
                            onPreviewViewReady = onPreviewViewReady,
                            modifier = Modifier.weight(1f),
                            onClearRecognizedText = onClearRecognizedText,
                            onSelectPhoto = onSelectPhoto
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MainContent(
                            inputMode = inputMode,
                            showLandmarks = showLandmarks,
                            onShowLandmarksChange = onShowLandmarksChange,
                            cameraFacing = cameraFacing,
                            onCameraFacingChange = onCameraFacingChange,
                            isTorchSupported = isTorchSupported,
                            isTorchEnabled = isTorchEnabled,
                            onTorchEnabledChange = onTorchEnabledChange,
                            recognizedText = recognizedText,
                            textInput = textInput,
                            onTextInputChange = onTextInputChange,
                            landmarks = landmarks,
                            onPreviewViewReady = onPreviewViewReady,
                            modifier = Modifier.weight(1f),
                            onClearRecognizedText = onClearRecognizedText,
                            onSelectPhoto = onSelectPhoto
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun MainContent(
    inputMode: InputMode,
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
    onSelectPhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {

        if (inputMode == InputMode.CAMERA) {

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
                onSelectPhoto = onSelectPhoto
            )

        } else {

            TextInputPanel(
                textInput = textInput,
                onTextInputChange = onTextInputChange
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        TranslationPanel(recognizedText, onClearRecognizedText)
    }
}






