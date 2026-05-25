package com.example.handtranslator.translator

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.example.handtranslator.HandLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class RecognitionManager(
    private val handLandmarker: HandLandmarkerHelper,
    private val engine: GestureRecognitionEngine
) {
    fun detect(imageProxy: ImageProxy): List<NormalizedLandmark> = handLandmarker.detect(imageProxy).orEmpty()

    fun recognize(bitmap: Bitmap, confidenceThreshold: Float): RecognitionOutcome {
        val detectedLandmarks = handLandmarker.detect(bitmap).orEmpty()
        val prediction = if (detectedLandmarks.isEmpty()) null else engine.predict(detectedLandmarks, confidenceThreshold)
        return RecognitionOutcome(detectedLandmarks, prediction)
    }

    fun recognize(landmarks: List<NormalizedLandmark>, confidenceThreshold: Float): RecognitionResult? {
        if (landmarks.isEmpty()) return null
        return engine.predict(landmarks, confidenceThreshold)
    }
}

data class RecognitionOutcome(
    val landmarks: List<NormalizedLandmark>,
    val result: RecognitionResult?
)
