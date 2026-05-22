package com.example.handtranslator.translator

import android.net.Uri
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.handtranslator.Helper.loadBitmapFromUri
import com.example.handtranslator.R

@Composable
fun MediaPanel(
    selectedMediaUri: Uri?,
    selectedMediaType: SelectedMediaType,
    videoPreviewFillEnabled: Boolean,
    onSelectMedia: (Uri) -> Unit,
    onSwitchToCameraPreview: () -> Unit,
) {
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onSelectMedia) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (selectedMediaUri != null && selectedMediaType != SelectedMediaType.NONE) {
            when (selectedMediaType) {
                SelectedMediaType.VIDEO -> SelectedVideoPreview(selectedMediaUri, videoPreviewFillEnabled)
                SelectedMediaType.PHOTO -> SelectedPhotoPreview(selectedMediaUri)
                else -> Unit
            }
        } else {
            EmptyMediaState {
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
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
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_to_camera))
                }

                IconButton(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Default.PhotoLibrary, stringResource(R.string.select_another_media))
                }
            }
        }
    }
}

@Composable
private fun EmptyMediaState(onSelectMedia: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(56.dp))
        Text(stringResource(R.string.media_empty))
        Button(onClick = onSelectMedia, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.open_gallery))
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
private fun SelectedVideoPreview(uri: Uri, videoPreviewFillEnabled: Boolean) {
    val context = LocalContext.current
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply { repeatMode = ExoPlayer.REPEAT_MODE_ALL }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        factory = {
            PlayerView(context).apply {
                this.player = player
                useController = true
                resizeMode = if (videoPreviewFillEnabled) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        },
        update = { playerView ->
            playerView.resizeMode = if (videoPreviewFillEnabled) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        }
    )
}
