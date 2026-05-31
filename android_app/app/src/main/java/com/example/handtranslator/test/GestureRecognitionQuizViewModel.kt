package com.example.handtranslator.test

import android.app.Application
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.example.handtranslator.AslClassifier
import com.example.handtranslator.HandLandmarkerHelper
import com.example.handtranslator.Helper.loadAslLabels
import com.example.handtranslator.translator.CameraFacing
import com.example.handtranslator.translator.GestureRecognitionEngine
import com.example.handtranslator.translator.PredictionStabilizer
import com.example.handtranslator.translator.RecognitionManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureRecognitionQuizViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val SLIDING_WINDOW_SIZE = 10
        const val REQUIRED_MATCHES = 2
        const val FRAME_SAMPLE_INTERVAL_MS = 90L
        const val PREDICTION_COOLDOWN_MS = 500L
        const val CONFIDENCE_THRESHOLD = 0.45f
    }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val classifier = AslClassifier(application.applicationContext)
    private val recognitionManager = RecognitionManager(
        handLandmarker = HandLandmarkerHelper(application.applicationContext),
        engine = GestureRecognitionEngine(classifier, loadAslLabels(application.applicationContext))
    )
    private val stabilizer = PredictionStabilizer(SLIDING_WINDOW_SIZE)
    private val remainingLetters = mutableListOf<String>()

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var surfaceProvider: Preview.SurfaceProvider? = null
    private var lastPredictionTime = 0L

    var uiState by mutableStateOf(GestureRecognitionQuizUiState())
        private set

    fun start(lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        remainingLetters.clear()
        uiState = GestureRecognitionQuizUiState(phase = GestureRecognitionQuizPhase.PLAYING)
        nextQuestionOrFinish()
        if (hasCameraPermission) bindCameraUseCases(lifecycleOwner)
    }

    fun onCameraSurfaceReady(provider: Preview.SurfaceProvider, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        surfaceProvider = provider
        if (uiState.phase == GestureRecognitionQuizPhase.PLAYING && hasCameraPermission) bindCameraUseCases(lifecycleOwner)
    }

    fun onCameraFacingChange(facing: CameraFacing, lifecycleOwner: LifecycleOwner) {
        uiState = uiState.copy(cameraFacing = facing)
        stabilizer.clear()
        if (uiState.phase == GestureRecognitionQuizPhase.PLAYING) bindCameraUseCases(lifecycleOwner)
    }

    fun onShowLandmarksChange(show: Boolean) {
        uiState = uiState.copy(showLandmarks = show)
    }

    fun onTorchEnabledChange(enabled: Boolean) {
        uiState = uiState.copy(isTorchEnabled = enabled)
        activeCamera?.cameraControl?.enableTorch(enabled)
    }

    fun stop() {
        cameraProvider?.unbindAll()
        activeCamera = null
        stabilizer.clear()
        uiState = uiState.copy(isTorchSupported = false, isTorchEnabled = false, landmarks = emptyList())
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val currentSurfaceProvider = surfaceProvider ?: return
        val future = ProcessCameraProvider.getInstance(getApplication())
        future.addListener({
            cameraProvider = future.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = currentSurfaceProvider }
            val analyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { image -> processFrameSafely(image) }
            }
            val selector = if (uiState.cameraFacing == CameraFacing.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider?.unbindAll()
            activeCamera = cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
            val torchSupported = activeCamera?.cameraInfo?.hasFlashUnit() == true
            uiState = uiState.copy(isTorchSupported = torchSupported, isTorchEnabled = uiState.isTorchEnabled && torchSupported)
            activeCamera?.cameraControl?.enableTorch(uiState.isTorchEnabled)
        }, androidx.core.content.ContextCompat.getMainExecutor(getApplication()))
    }

    private fun processFrameSafely(imageProxy: ImageProxy) {
        try {
            if (uiState.phase != GestureRecognitionQuizPhase.PLAYING) return
            val detectedLandmarks = recognitionManager.detect(imageProxy)
            uiState = uiState.copy(landmarks = detectedLandmarks)
            if (detectedLandmarks.isEmpty()) {
                stabilizer.clear()
                return
            }
            val now = System.currentTimeMillis()
            if (now - lastPredictionTime < PREDICTION_COOLDOWN_MS) return
            if (!stabilizer.shouldSample(now, FRAME_SAMPLE_INTERVAL_MS)) return
            val prediction = recognitionManager.recognize(detectedLandmarks, CONFIDENCE_THRESHOLD) ?: return
            stabilizer.add(prediction.letter, now)
            stabilizer.resolve(REQUIRED_MATCHES)?.let { stableLetter ->
                lastPredictionTime = now
                handlePrediction(stableLetter)
            }
        } catch (e: Exception) {
            Log.e("GestureQuiz", "Analyzer error", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun handlePrediction(letter: String) {
        val normalized = letter.uppercase()
        val isCorrect = normalized == uiState.currentLetter
        if (isCorrect) {
            uiState = uiState.copy(
                score = uiState.score + 1,
                attempts = uiState.attempts + 1,
                status = GestureRecognitionQuizStatus.CORRECT,
                lastPrediction = normalized
            )
            stabilizer.clear()
            nextQuestionOrFinish()
        } else {
            uiState = uiState.copy(
                attempts = uiState.attempts + 1,
                mistakes = uiState.mistakes + 1,
                status = GestureRecognitionQuizStatus.WRONG,
                lastPrediction = normalized
            )
            stabilizer.clear()
        }
    }

    private fun nextQuestionOrFinish() {
        if (uiState.questionIndex >= uiState.totalQuestions) {
            uiState = uiState.copy(phase = GestureRecognitionQuizPhase.FINISHED, status = GestureRecognitionQuizStatus.IDLE, landmarks = emptyList())
            stop()
            return
        }
        if (remainingLetters.isEmpty()) remainingLetters.addAll(deck.map { it.letter }.shuffled())
        uiState = uiState.copy(
            currentLetter = remainingLetters.removeAt(0),
            questionIndex = uiState.questionIndex + 1,
            status = GestureRecognitionQuizStatus.IDLE,
            lastPrediction = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        stop()
        cameraExecutor.shutdown()
        classifier.close()
    }
}
