package com.example.handtranslator.translator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.zIndex
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.collections.forEach

@Composable
fun CameraPanel(
    showLandmarks: Boolean,
    onShowLandmarksChange: (Boolean) -> Unit,
    cameraFacing: CameraFacing,
    onCameraFacingChange: (CameraFacing) -> Unit,
    isTorchSupported: Boolean,
    isTorchEnabled: Boolean,
    onTorchEnabledChange: (Boolean) -> Unit,
    landmarks: List<NormalizedLandmark>,
    onPreviewViewReady: (PreviewView) -> Unit,
    onSelectMedia: (Uri) -> Unit
) {
    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectMedia(uri)
        }
    }

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
                    val landmarkX = if (cameraFacing == CameraFacing.FRONT) 1f - point.x() else point.x()
                    drawCircle(
                        color = Color.Green,
                        radius = 7f,
                        center = Offset(landmarkX * size.width, point.y() * size.height)
                    )
                }
            }
        }

        val iconBackgroundColor by animateColorAsState(
            targetValue = if (!controlsVisible) Color.Black.copy(alpha = 0.45f) else Color.Transparent,
            animationSpec = tween(durationMillis = 220),
            label = "cameraMenuIconBackground"
        )

        IconButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .zIndex(2f)
                .background(iconBackgroundColor, RoundedCornerShape(12.dp)),
            onClick = { controlsVisible = !controlsVisible }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Скрыть панель",
                tint = Color.White
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        }
                    ) {
                        Text("Выбрать фото/видео")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Показывать точки MediaPipe", color = Color.White)
                        Switch(checked = showLandmarks, onCheckedChange = onShowLandmarksChange)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Фонарик", color = Color.White)
                        Switch(checked = isTorchEnabled,
                            onCheckedChange = onTorchEnabledChange,
                            enabled = cameraFacing == CameraFacing.BACK && isTorchSupported
                        )
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
}