package com.example.handtranslator

import android.R.attr.bitmap
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

class HandLandmarkerHelper(context: Context) {

    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .setDelegate(Delegate.GPU)
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(1)
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }


    @OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy): List<NormalizedLandmark>? {
        val bitmap = imageProxy.toBitmap() ?: return null

        val matrix = android.graphics.Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap.copy(Bitmap.Config.ARGB_8888, false),
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        return detect(rotatedBitmap)
    }

    fun detect(bitmap: Bitmap): List<NormalizedLandmark>? {
        val rgbaBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val mpImage = BitmapImageBuilder(rgbaBitmap).build()
        val result = handLandmarker.detect(mpImage)

        if (result.landmarks().isEmpty()) return null

        return result.landmarks().first()
    }

}

