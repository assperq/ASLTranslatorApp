package com.example.handtranslator.test

import com.example.handtranslator.translator.CameraFacing
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

const val GESTURE_QUIZ_QUESTION_COUNT = 10

data class GestureRecognitionQuizUiState(
    val phase: GestureRecognitionQuizPhase = GestureRecognitionQuizPhase.READY,
    val currentLetter: String = "A",
    val questionIndex: Int = 0,
    val totalQuestions: Int = GESTURE_QUIZ_QUESTION_COUNT,
    val score: Int = 0,
    val attempts: Int = 0,
    val mistakes: Int = 0,
    val status: GestureRecognitionQuizStatus = GestureRecognitionQuizStatus.IDLE,
    val lastPrediction: String? = null,
    val landmarks: List<NormalizedLandmark> = emptyList(),
    val showLandmarks: Boolean = false,
    val cameraFacing: CameraFacing = CameraFacing.FRONT,
    val isTorchSupported: Boolean = false,
    val isTorchEnabled: Boolean = false,
) {
    val isFinished: Boolean get() = phase == GestureRecognitionQuizPhase.FINISHED
}

enum class GestureRecognitionQuizPhase { READY, PLAYING, FINISHED }
enum class GestureRecognitionQuizStatus { IDLE, CORRECT, WRONG }
