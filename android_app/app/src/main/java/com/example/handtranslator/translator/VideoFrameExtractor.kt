package com.example.handtranslator.translator

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoFrameExtractor(
    private val application: Application
) {
    fun forEachFrame(uri: Uri, intervalMs: Long, onFrame: (Bitmap) -> Unit) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(application, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            var frameTimeMs = 0L
            while (frameTimeMs <= durationMs) {
                val frame = retriever.getFrameAtTime(frameTimeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) onFrame(frame)
                frameTimeMs += intervalMs
            }
        } finally {
            retriever.release()
        }
    }

    fun extractSingleFrame(uri: Uri, positionMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(application, uri)
            retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
        } finally {
            retriever.release()
        }
    }
}
