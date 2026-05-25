package com.example.handtranslator.translator

import com.example.handtranslator.AslClassifier
import com.example.handtranslator.Helper.landmarksTo210Features
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class GestureRecognitionEngine(
    private val classifier: AslClassifier,
    private val labels: List<String>
) {
    fun predict(detectedLandmarks: List<NormalizedLandmark>, confidenceThreshold: Float): RecognitionResult? {
        val features = landmarksTo210Features(detectedLandmarks)
        val prediction = classifier.predict(features)
        if (prediction.index !in labels.indices || prediction.confidence < confidenceThreshold) return null
        return RecognitionResult(letter = labels[prediction.index], confidence = prediction.confidence)
    }
}

data class RecognitionResult(
    val letter: String,
    val confidence: Float
)
