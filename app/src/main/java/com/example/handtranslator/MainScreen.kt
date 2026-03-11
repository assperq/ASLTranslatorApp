package com.example.handtranslator

import android.R.attr.onClick
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

enum class InputMode {
    CAMERA,
    TEXT
}

enum class CameraFacing {
    FRONT,
    BACK
}

@Composable
fun MainScreen(
    inputMode: InputMode,
    onInputModeChange: (InputMode) -> Unit,
    showLandmarks: Boolean,
    onShowLandmarksChange: (Boolean) -> Unit,
    cameraFacing: CameraFacing,
    onCameraFacingChange: (CameraFacing) -> Unit,
    recognizedText: String,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    landmarks: List<NormalizedLandmark>,
    onPreviewViewReady: (PreviewView) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            InputModeSelector(inputMode = inputMode, onInputModeChange = onInputModeChange)
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.35f),
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
                            onPreviewViewReady = onPreviewViewReady
                        )
                    } else {
                        TextInputPanel(textInput = textInput, onTextInputChange = onTextInputChange)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TranslationPanel(recognizedText = recognizedText)
                }
            }
        }
    }
}

@Composable
private fun InputModeSelector(inputMode: InputMode, onInputModeChange: (InputMode) -> Unit) {
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

@Composable
private fun CameraPanel(
    showLandmarks: Boolean,
    onShowLandmarksChange: (Boolean) -> Unit,
    cameraFacing: CameraFacing,
    onCameraFacingChange: (CameraFacing) -> Unit,
    landmarks: List<NormalizedLandmark>,
    onPreviewViewReady: (PreviewView) -> Unit
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    onPreviewViewReady(this)
                }
            }
        )

        if (landmarks.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                landmarks.forEach { point ->
                    val landmarkX = if (cameraFacing == CameraFacing.FRONT) {
                        1f - point.x()
                    } else {
                        point.x()
                    }
                    drawCircle(
                        color = Color.Green,
                        radius = 7f,
                        center = Offset(landmarkX * size.width, point.y() * size.height)
                    )
                }
            }
        }

        var iconModifier = Modifier.align(Alignment.TopEnd)
            .padding(8.dp)
        iconModifier = if (!controlsVisible)
                iconModifier.background(Color.Black.copy(alpha = 0.45f),
                    RoundedCornerShape(12.dp))
            else iconModifier

        IconButton(
            modifier = iconModifier,
            onClick = { controlsVisible = !controlsVisible }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Скрыть панель",
                tint = Color.White
            )
        }

        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Выбор фото/видео") }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Показывать точки MediaPipe", color = Color.White)
                    Switch(checked = showLandmarks, onCheckedChange = onShowLandmarksChange)
                }

                FilterChip(
                    selected = cameraFacing == CameraFacing.FRONT,
                    onClick = {
                        onCameraFacingChange(
                            if (cameraFacing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
                        )
                    },
                    label = {
                        Text(
                            if (cameraFacing == CameraFacing.FRONT) "Фронтальная камера" else "Задняя камера"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TextInputPanel(textInput: String, onTextInputChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Введите текст для перевода", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textInput,
            onValueChange = onTextInputChange,
            label = { Text("Текст") }
        )
    }
}

@Composable
private fun TranslationPanel(recognizedText: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Перевод", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(recognizedText, style = MaterialTheme.typography.displaySmall)
    }
}
