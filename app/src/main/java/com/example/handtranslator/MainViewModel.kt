package com.example.handtranslator

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.handtranslator.Helper.landmarksTo210Features
import com.example.handtranslator.Helper.loadAslLabels
import com.example.handtranslator.translator.CameraFacing
import com.example.handtranslator.translator.InputMode
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handLandmarkerHelper = HandLandmarkerHelper(application.applicationContext)
    private val aslClassifier = AslClassifier(application.applicationContext)
    private val aslLabels by lazy { loadAslLabels(application.applicationContext) }
    private var lastPredictionTime = 0L

    var inputMode by mutableStateOf(InputMode.CAMERA)
        private set
    var showLandmarks by mutableStateOf(false)
        private set
    var cameraFacing by mutableStateOf(CameraFacing.FRONT)
        private set
    var recognizedText by mutableStateOf("Перевод появится здесь")
        private set
    var textInput by mutableStateOf("")
        private set
    var landmarks by mutableStateOf<List<NormalizedLandmark>>(emptyList())
        private set

    fun onInputModeChange(mode: InputMode, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        inputMode = mode
        if (mode == InputMode.CAMERA && hasCameraPermission) {
            bindCameraUseCases(lifecycleOwner)
        } else if (mode != InputMode.CAMERA) {
            stopCamera()
            landmarks = emptyList()
        }
    }

    fun onShowLandmarksChange(show: Boolean) {
        showLandmarks = show
    }

    fun onCameraFacingChange(facing: CameraFacing, lifecycleOwner: LifecycleOwner) {
        cameraFacing = facing
        if (inputMode == InputMode.CAMERA) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onTextInputChange(text: String) {
        textInput = text
        recognizedText = text
    }

    fun onPreviewViewReady(view: PreviewView, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        previewView = view
        if (inputMode == InputMode.CAMERA && hasCameraPermission) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onCameraPermissionGranted(lifecycleOwner: LifecycleOwner) {
        if (inputMode == InputMode.CAMERA) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val currentPreviewView = previewView ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = currentPreviewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            processFrame(imageProxy)
                        } catch (e: Exception) {
                            Log.e("Camera", "Analyzer error", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = when (cameraFacing) {
                CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
        }, androidx.core.content.ContextCompat.getMainExecutor(getApplication()))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val detectedLandmarks = handLandmarkerHelper.detect(imageProxy)
        viewModelScope.launch(Dispatchers.Main) {
            landmarks = detectedLandmarks.orEmpty()
        }

        if (!detectedLandmarks.isNullOrEmpty()) {
            processFrameWithPrediction(detectedLandmarks)
        }
    }

    private fun processFrameWithPrediction(detectedLandmarks: List<NormalizedLandmark>) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPredictionTime < 1500) return

        lastPredictionTime = currentTime

        viewModelScope.launch(Dispatchers.Default) {
            val features = landmarksTo210Features(detectedLandmarks)
            val predictedIndex = aslClassifier.predict(features)
            val predictedLetter = if (predictedIndex in aslLabels.indices) {
                aslLabels[predictedIndex]
            } else {
                "?"
            }
            withContext(Dispatchers.Main) {
                recognizedText = predictedLetter
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCamera()
        cameraExecutor.shutdown()
    }
}
