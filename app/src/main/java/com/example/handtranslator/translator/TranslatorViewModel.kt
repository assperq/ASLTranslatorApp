package com.example.handtranslator.translator

import com.example.handtranslator.AslClassifier
import com.example.handtranslator.HandLandmarkerHelper
import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
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
import com.example.handtranslator.Helper.loadBitmapFromUri
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val PREDICTION_COOLDOWN_MS = 1500L
        const val SLIDING_WINDOW_SIZE = 5
        const val REQUIRED_MATCHES = 4
        const val FRAME_SAMPLE_INTERVAL_MS = 200L
        const val CONFIDENCE_THRESHOLD = 0.7f
    }

    private data class PendingPrediction(
        val letter: String
    )

    private var previewView: WeakReference<PreviewView?> = WeakReference(null)
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handLandmarkerHelper = HandLandmarkerHelper(application.applicationContext)
    private val aslClassifier = AslClassifier(application.applicationContext)
    private val aslLabels by lazy { loadAslLabels(application.applicationContext) }
    private var lastPredictionTime = 0L
    private var activeCamera: Camera? = null
    private var mediaProcessingJob: Job? = null
    private val pendingPredictions = ArrayDeque<PendingPrediction>()
    private var lastSampledFrameTime = 0L

    var isTorchSupported by mutableStateOf(false)
        private set
    var isTorchEnabled by mutableStateOf(false)
        private set

    var inputMode by mutableStateOf(InputMode.CAMERA)
        private set
    var cameraContentMode by mutableStateOf(CameraContentMode.LIVE_CAMERA)
        private set
    var selectedMediaUri by mutableStateOf<Uri?>(null)
        private set
    var selectedMediaType by mutableStateOf(SelectedMediaType.NONE)
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
        if (mode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA && hasCameraPermission) {
            bindCameraUseCases(lifecycleOwner)
        } else {
            stopCamera()
            mediaProcessingJob?.cancel()
            landmarks = emptyList()
            resetSlidingWindowState()
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
        try {
            val newLetter = Letter(
                name = recognizedLetter,
                imageCard = getAslDrawable(getApplication(), recognizedLetter)
            )
            recognizedText = recognizedText + newLetter
        } catch (_: Exception) {}
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
        resetSlidingWindowState()
        if (inputMode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onTextInputChange(text: String) {
        textInput = text
        recognizedText = emptyList()
        text.forEach { onRecognizeLetter(it.toString()) }
    }

    fun onPreviewViewReady(view: PreviewView, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        previewView = WeakReference(view)
        if (inputMode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA && hasCameraPermission) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onCameraPermissionGranted(lifecycleOwner: LifecycleOwner) {
        if (inputMode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun onSelectMedia(uri: Uri) {
        selectedMediaUri = uri
        selectedMediaType = resolveMediaType(uri)
        cameraContentMode = CameraContentMode.SELECTED_MEDIA
        stopCamera()
        landmarks = emptyList()
        recognizedText = emptyList()
        mediaProcessingJob?.cancel()
        resetSlidingWindowState()

        when (selectedMediaType) {
            SelectedMediaType.PHOTO -> processPhoto(uri)
            SelectedMediaType.VIDEO -> processVideo(uri)
            SelectedMediaType.NONE -> Unit
        }
    }

    fun onSwitchToCameraPreview(lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        cameraContentMode = CameraContentMode.LIVE_CAMERA
        landmarks = emptyList()
        mediaProcessingJob?.cancel()
        resetSlidingWindowState()
        if (inputMode == InputMode.CAMERA && hasCameraPermission) {
            bindCameraUseCases(lifecycleOwner)
        }
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        activeCamera = null
        isTorchSupported = false
        isTorchEnabled = false
        resetSlidingWindowState()
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

        if (detectedLandmarks.isNullOrEmpty()) {
            resetSlidingWindowState()
            return
        }

        processLandmarksWithPrediction(detectedLandmarks)
    }

    private fun processLandmarksWithPrediction(detectedLandmarks: List<NormalizedLandmark>) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPredictionTime < PREDICTION_COOLDOWN_MS) return
        if (pendingPredictions.isNotEmpty() && currentTime - lastSampledFrameTime < FRAME_SAMPLE_INTERVAL_MS) return

        collectPredictionSample(detectedLandmarks, currentTime)
    }

    private fun collectPredictionSample(
        detectedLandmarks: List<NormalizedLandmark>,
        sampleTimeMs: Long
    ) {
        val prediction = predictLetter(detectedLandmarks) ?: return
        pendingPredictions.addLast(prediction)
        lastSampledFrameTime = sampleTimeMs

        while (pendingPredictions.size > SLIDING_WINDOW_SIZE) {
            pendingPredictions.removeFirst()
        }

        if (pendingPredictions.size < SLIDING_WINDOW_SIZE) return
        val majorityPrediction = pendingPredictions
            .groupingBy { it.letter }
            .eachCount()
            .maxByOrNull { it.value }

        if (majorityPrediction != null && majorityPrediction.value >= REQUIRED_MATCHES) {
            lastPredictionTime = sampleTimeMs
            pendingPredictions.clear()
            viewModelScope.launch(Dispatchers.Main) {
                onRecognizeLetter(majorityPrediction.key)
            }
        }
    }

    private fun resetSlidingWindowState() {
        pendingPredictions.clear()
        lastSampledFrameTime = 0L
    }

    private fun processPhoto(uri: Uri) {
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val bitmap = loadBitmapFromUri(getApplication(), uri) ?: return@launch
            processBitmapForPrediction(bitmap, updateLandmarks = true)
        }
    }

    private fun processVideo(uri: Uri) {
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(getApplication(), uri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                var frameTimeMs = 0L
                while (frameTimeMs <= durationMs) {
                    val frameBitmap = retriever.getFrameAtTime(
                        frameTimeMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (frameBitmap != null) {
                        processBitmapForPrediction(frameBitmap, updateLandmarks = false)
                    }
                    frameTimeMs += 1500L
                }
            } catch (e: Exception) {
                Log.e("Media", "Failed to process video frames", e)
            } finally {
                retriever.release()
            }
        }
    }

    private suspend fun processBitmapForPrediction(bitmap: Bitmap, updateLandmarks: Boolean) {
        val detectedLandmarks = handLandmarkerHelper.detect(bitmap)
        if (updateLandmarks) {
            withContext(Dispatchers.Main) {
                landmarks = detectedLandmarks.orEmpty()
            }
        }
        if (!detectedLandmarks.isNullOrEmpty()) {
            val predictedLetter = predictLetter(detectedLandmarks)?.letter ?: return
            withContext(Dispatchers.Main) {
                onRecognizeLetter(predictedLetter)
            }
        }
    }

    private fun predictLetter(detectedLandmarks: List<NormalizedLandmark>): PendingPrediction? {
        val features = landmarksTo210Features(detectedLandmarks)
        val prediction = aslClassifier.predict(features)
        if (prediction.index !in aslLabels.indices || prediction.confidence < CONFIDENCE_THRESHOLD) {
            return null
        }
        return PendingPrediction(letter = aslLabels[prediction.index])
    }

    private fun resolveMediaType(uri: Uri): SelectedMediaType {
        val mimeType = getApplication<Application>().contentResolver.getType(uri).orEmpty()
        return when {
            mimeType.startsWith("image/") -> SelectedMediaType.PHOTO
            mimeType.startsWith("video/") -> SelectedMediaType.VIDEO
            else -> SelectedMediaType.NONE
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCamera()
        mediaProcessingJob?.cancel()
        cameraExecutor.shutdown()
        aslClassifier.close()
    }
}
