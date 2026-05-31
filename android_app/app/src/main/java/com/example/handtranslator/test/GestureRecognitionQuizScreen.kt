package com.example.handtranslator.test

import androidx.camera.core.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.example.handtranslator.R
import com.example.handtranslator.translator.CameraPanel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GestureRecognitionQuizScreen(
    onBack: () -> Unit,
    lifecycleOwner: LifecycleOwner,
    hasCameraPermission: Boolean,
    ensureCameraPermission: () -> Unit,
    viewModel: GestureRecognitionQuizViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState

    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Surface(modifier = Modifier.fillMaxSize().padding(12.dp), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_main))
                }
                Text(
                    text = stringResource(R.string.gesture_quiz_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            when (uiState.phase) {
                GestureRecognitionQuizPhase.READY -> ReadyQuizCard {
                    viewModel.start(lifecycleOwner, hasCameraPermission)
                    if (!hasCameraPermission) ensureCameraPermission()
                }
                GestureRecognitionQuizPhase.PLAYING -> PlayingQuizContent(
                    uiState = uiState,
                    onPreviewSurfaceReady = { provider: Preview.SurfaceProvider ->
                        viewModel.onCameraSurfaceReady(provider, lifecycleOwner, hasCameraPermission)
                        if (!hasCameraPermission) ensureCameraPermission()
                    },
                    onShowLandmarksChange = viewModel::onShowLandmarksChange,
                    onCameraFacingChange = { viewModel.onCameraFacingChange(it, lifecycleOwner) },
                    onTorchEnabledChange = viewModel::onTorchEnabledChange
                )
                GestureRecognitionQuizPhase.FINISHED -> FinishedQuizCard(uiState = uiState) {
                    viewModel.start(lifecycleOwner, hasCameraPermission)
                    if (!hasCameraPermission) ensureCameraPermission()
                }
            }
        }
    }
}

@Composable
private fun ReadyQuizCard(onStart: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 5.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.gesture_quiz_ready_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.gesture_quiz_ready_subtitle), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
            Button(onClick = onStart) { Text(stringResource(R.string.game_start_button)) }
        }
    }
}

@Composable
private fun PlayingQuizContent(
    uiState: GestureRecognitionQuizUiState,
    onPreviewSurfaceReady: (Preview.SurfaceProvider) -> Unit,
    onShowLandmarksChange: (Boolean) -> Unit,
    onCameraFacingChange: (com.example.handtranslator.translator.CameraFacing) -> Unit,
    onTorchEnabledChange: (Boolean) -> Unit,
) {
    GestureScorePill(stringResource(R.string.gesture_quiz_score, uiState.score, uiState.questionIndex, uiState.totalQuestions))

    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.gesture_quiz_show_letter), color = MaterialTheme.colorScheme.secondary)
            Text(uiState.currentLetter, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            val statusText = when (uiState.status) {
                GestureRecognitionQuizStatus.IDLE -> stringResource(R.string.gesture_quiz_camera_hint)
                GestureRecognitionQuizStatus.CORRECT -> stringResource(R.string.answer_correct)
                GestureRecognitionQuizStatus.WRONG -> stringResource(R.string.gesture_quiz_wrong, uiState.lastPrediction.orEmpty())
            }
            val statusColor = when (uiState.status) {
                GestureRecognitionQuizStatus.CORRECT -> Color(0xFF1E7F41)
                GestureRecognitionQuizStatus.WRONG -> Color(0xFF9D1D1D)
                GestureRecognitionQuizStatus.IDLE -> MaterialTheme.colorScheme.secondary
            }
            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }

    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 4.dp, shape = RoundedCornerShape(20.dp)) {
        CameraPanel(
            showLandmarks = uiState.showLandmarks,
            onShowLandmarksChange = onShowLandmarksChange,
            cameraFacing = uiState.cameraFacing,
            onCameraFacingChange = onCameraFacingChange,
            landmarks = if (uiState.showLandmarks) uiState.landmarks else emptyList(),
            onPreviewSurfaceReady = onPreviewSurfaceReady,
            isTorchSupported = uiState.isTorchSupported,
            isTorchEnabled = uiState.isTorchEnabled,
            onTorchEnabledChange = onTorchEnabledChange,
            onSelectMedia = {}
        )
    }
}

@Composable
private fun FinishedQuizCard(uiState: GestureRecognitionQuizUiState, onRestart: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 5.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.gesture_quiz_finished), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.gesture_quiz_result, uiState.score, uiState.totalQuestions, uiState.mistakes), textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRestart) { Text(stringResource(R.string.practice_restart)) }
        }
    }
}

@Composable
private fun GestureScorePill(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}
