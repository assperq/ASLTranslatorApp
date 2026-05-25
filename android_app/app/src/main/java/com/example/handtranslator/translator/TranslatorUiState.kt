package com.example.handtranslator.translator

import android.net.Uri
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class TranslatorUiState(
    val isTorchSupported: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val inputMode: InputMode = InputMode.CAMERA,
    val cameraContentMode: CameraContentMode = CameraContentMode.LIVE_CAMERA,
    val selectedMediaUri: Uri? = null,
    val selectedMediaType: SelectedMediaType = SelectedMediaType.NONE,
    val showLandmarks: Boolean = false,
    val cameraFacing: CameraFacing = CameraFacing.FRONT,
    val recognizedText: List<Letter> = emptyList(),
    val textInput: String = "",
    val landmarks: List<NormalizedLandmark> = emptyList(),
    val singleFrameRecognitionResult: Letter? = null,
    val isSingleFrameRecognizing: Boolean = false,
    val singleFrameRecognitionFailed: Boolean = false,
)
