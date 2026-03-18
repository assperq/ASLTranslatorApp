package com.example.handtranslator.translator

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.handtranslator.Helper.loadBitmapFromUri

@Composable
fun MediaPanel(
    selectedMediaUri: Uri?,
    selectedMediaType: SelectedMediaType,
    onSelectMedia: (Uri) -> Unit,
    onSwitchToCameraPreview: () -> Unit,
) {
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onSelectMedia)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        if (selectedMediaUri != null && selectedMediaType != SelectedMediaType.NONE) {
            when (selectedMediaType) {
                SelectedMediaType.VIDEO -> {
                    SelectedVideoPreview(selectedMediaUri)
                }

                SelectedMediaType.PHOTO -> {
                    SelectedPhotoPreview(selectedMediaUri)
                }

                else -> {}
            }
        } else {
            EmptyMediaState {
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )
            }
        }

        if (selectedMediaUri != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                IconButton(
                    onClick = onSwitchToCameraPreview,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }

                IconButton(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        )
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EmptyMediaState(onSelectMedia: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
        Text("Выберите фото или видео для распознавания")
        Button(onClick = onSelectMedia, modifier = Modifier.padding(top = 12.dp)) {
            Text("Открыть галерею")
        }
    }
}

@Composable
private fun SelectedPhotoPreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) { loadBitmapFromUri(context, uri) }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SelectedVideoPreview(uri: Uri) {
    val context = LocalContext.current
    val mediaController = remember(context) { MediaController(context) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        factory = {
            VideoView(context).apply {
                setVideoURI(uri)
                setMediaController(mediaController)
                mediaController.setAnchorView(this)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    start()
                }
            }
        },
        update = { videoView ->
            videoView.setVideoURI(uri)
            videoView.setMediaController(mediaController)
            mediaController.setAnchorView(videoView)
            videoView.seekTo(1)
            videoView.start()
        }
    )
}