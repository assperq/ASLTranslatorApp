package com.example.handtranslator.translator

import com.example.handtranslator.AslClassifier
import com.example.handtranslator.HandLandmarkerHelper
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
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
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.handtranslator.Helper.getAslDrawable
import com.example.handtranslator.Helper.landmarksTo210Features
import com.example.handtranslator.Helper.loadAslLabels
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private var previewView: WeakReference<PreviewView?> = WeakReference(null)
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handLandmarkerHelper = HandLandmarkerHelper(application.applicationContext)
    private val aslClassifier = AslClassifier(application.applicationContext)
    private val aslLabels by lazy { loadAslLabels(application.applicationContext) }
    private var lastPredictionTime = 0L
    private var activeCamera: Camera? = null

    var isTorchSupported by mutableStateOf(false)
        private set
    var isTorchEnabled by mutableStateOf(false)
        private set

    var inputMode by mutableStateOf(InputMode.CAMERA)
        private set
    var showLandmarks by mutableStateOf(false)
        private set
    var cameraFacing by mutableStateOf(CameraFacing.FRONT)
        private set
    var recognizedText by mutableStateOf(emptyList<Letter>())
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

    fun onClearRecognizedText(oneLetter: Boolean) {
        recognizedText = if (oneLetter) {
            val newList = recognizedText.toMutableList()
            newList.remove(recognizedText.last())
            newList
        }
        else {
            emptyList()
        }
    }

    fun onRecognizeLetter(recognizedLetter : String) {
        val newLetter = Letter(
            name = recognizedLetter,
            imageCard = getAslDrawable(getApplication(), recognizedLetter)
        )
        val newList = recognizedText.toMutableList()
        newList.add(newLetter)
        recognizedText = newList
    }

    fun onTorchEnabledChange(enabled: Boolean) {
        isTorchEnabled = enabled
        activeCamera?.cameraControl?.enableTorch(enabled)
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
        text.forEach {
            onRecognizeLetter(it.toString())
        }
    }

    fun onPreviewViewReady(view: PreviewView, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        previewView = WeakReference(view)
        if (inputMode == InputMode.CAMERA && hasCameraPermission) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onCameraPermissionGranted(lifecycleOwner: LifecycleOwner) {
        if (inputMode == InputMode.CAMERA) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onSelectPhoto(uri: Uri) {

    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        activeCamera = null
        isTorchSupported = false
        isTorchEnabled = false
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val currentPreviewView = previewView.get() ?: return
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
            activeCamera = cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
            isTorchSupported = activeCamera?.cameraInfo?.hasFlashUnit() == true
            if (!isTorchSupported) {
                isTorchEnabled = false
            }
            activeCamera?.cameraControl?.enableTorch(isTorchEnabled && isTorchSupported)
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
                onRecognizeLetter(predictedLetter)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCamera()
        cameraExecutor.shutdown()
    }
}
