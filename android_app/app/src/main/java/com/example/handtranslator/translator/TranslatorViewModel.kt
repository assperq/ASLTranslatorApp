package com.example.handtranslator.translator

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.lifecycle.viewModelScope
import com.example.handtranslator.AslClassifier
import com.example.handtranslator.HandLandmarkerHelper
import com.example.handtranslator.Helper.getAslDrawable
import com.example.handtranslator.Helper.loadAslLabels
import com.example.handtranslator.Helper.loadBitmapFromUri
import com.example.handtranslator.data.preferences.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {
    private companion object { const val SLIDING_WINDOW_SIZE = 3 }

    private val settingsRepository = TranslatorSettingsRepository(DataStoreManager(application))
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val classifier = AslClassifier(application.applicationContext)
    private val recognitionManager = RecognitionManager(
        handLandmarker = HandLandmarkerHelper(application.applicationContext),
        engine = GestureRecognitionEngine(classifier, loadAslLabels(application.applicationContext))
    )
    private val stabilizer = PredictionStabilizer(SLIDING_WINDOW_SIZE)
    private val videoExtractor = VideoFrameExtractor(application)

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var surfaceProvider: Preview.SurfaceProvider? = null
    private var lastPredictionTime = 0L
    private var mediaProcessingJob: Job? = null

    var uiState by mutableStateOf(TranslatorUiState())
        private set

    val predictionCooldown = settingsRepository.predictionCooldown().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 400L)
    fun setPredictionCooldown(v: Long) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setPredictionCooldown(v) }
    val requiredMatches = settingsRepository.requiredMatches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)
    fun setRequiredMatches(v: Int) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setRequiredMatches(v) }
    val frameSampleIntervalMs = settingsRepository.frameSampleIntervalMs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90L)
    fun setFrameSampleIntervalMs(v: Long) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setFrameSampleIntervalMs(v) }
    val liveConfidenceThreshold = settingsRepository.liveConfidenceThreshold().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.45f)
    fun setLiveConfidenceThreshold(v: Float) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setLiveConfidenceThreshold(v) }
    val photoConfidenceThreshold = settingsRepository.photoConfidenceThreshold().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.35f)
    fun setPhotoConfidenceThreshold(v: Float) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setPhotoConfidenceThreshold(v) }
    val videoConfidenceThreshold = settingsRepository.videoConfidenceThreshold().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.2f)
    fun setVideoConfidenceThreshold(v: Float) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setVideoConfidenceThreshold(v) }
    val videoFrameSampleIntervalMs = settingsRepository.videoFrameSampleIntervalMs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1500L)
    fun setVideoFrameSampleIntervalMs(v: Long) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setVideoFrameSampleIntervalMs(v) }
    val videoPreviewFillEnabled = settingsRepository.videoPreviewFillEnabled().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setVideoPreviewFillEnabled(v: Boolean) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setVideoPreviewFillEnabled(v) }
    val singleFrameRecognitionTimeoutMs = settingsRepository.singleFrameRecognitionTimeoutMs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2500L)
    fun setSingleFrameRecognitionTimeoutMs(v: Long) = viewModelScope.launch(Dispatchers.IO) { settingsRepository.setSingleFrameRecognitionTimeoutMs(v) }

    fun onInputModeChange(mode: InputMode, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        uiState = uiState.copy(inputMode = mode)
        if (mode == InputMode.CAMERA && uiState.cameraContentMode == CameraContentMode.LIVE_CAMERA && hasCameraPermission) bindCameraUseCases(lifecycleOwner)
        else clearCameraFlow()
    }

    fun onCameraSurfaceReady(provider: Preview.SurfaceProvider, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        surfaceProvider = provider
        if (uiState.inputMode == InputMode.CAMERA && uiState.cameraContentMode == CameraContentMode.LIVE_CAMERA && hasCameraPermission) bindCameraUseCases(lifecycleOwner)
    }

    fun onCameraPermissionGranted(lifecycleOwner: LifecycleOwner) {
        if (uiState.inputMode == InputMode.CAMERA && uiState.cameraContentMode == CameraContentMode.LIVE_CAMERA) bindCameraUseCases(lifecycleOwner)
    }

    fun onCameraFacingChange(facing: CameraFacing, lifecycleOwner: LifecycleOwner) {
        uiState = uiState.copy(cameraFacing = facing)
        stabilizer.clear()
        if (uiState.inputMode == InputMode.CAMERA && uiState.cameraContentMode == CameraContentMode.LIVE_CAMERA) bindCameraUseCases(lifecycleOwner)
    }

    fun onTorchEnabledChange(enabled: Boolean) {
        uiState = uiState.copy(isTorchEnabled = enabled)
        activeCamera?.cameraControl?.enableTorch(enabled)
    }

    fun onShowLandmarksChange(show: Boolean) { uiState = uiState.copy(showLandmarks = show) }
    fun onClearRecognizedText(oneLetter: Boolean) { uiState = uiState.copy(recognizedText = if (oneLetter) uiState.recognizedText.dropLast(1) else emptyList()) }
    fun onTextInputChange(text: String) { uiState = uiState.copy(textInput = text, recognizedText = text.map { ch -> ch.toString().let { Letter(it, getAslDrawable(getApplication(), it)) } }) }

    fun onSelectMedia(uri: Uri) {
        uiState = uiState.copy(selectedMediaUri = uri, selectedMediaType = resolveMediaType(uri), cameraContentMode = CameraContentMode.SELECTED_MEDIA, recognizedText = emptyList())
        clearCameraFlow()
        mediaProcessingJob?.cancel()
        when (uiState.selectedMediaType) {
            SelectedMediaType.PHOTO -> processPhoto(uri)
            SelectedMediaType.VIDEO -> processVideo(uri)
            SelectedMediaType.NONE -> Unit
        }
    }

    fun onSwitchToCameraPreview(lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) {
        uiState = uiState.copy(cameraContentMode = CameraContentMode.LIVE_CAMERA, landmarks = emptyList())
        mediaProcessingJob?.cancel()
        stabilizer.clear()
        if (uiState.inputMode == InputMode.CAMERA && hasCameraPermission) bindCameraUseCases(lifecycleOwner)
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        activeCamera = null
        uiState = uiState.copy(isTorchSupported = false, isTorchEnabled = false)
        stabilizer.clear()
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
            val detectedLandmarks = recognitionManager.detect(imageProxy)
            uiState = uiState.copy(landmarks = detectedLandmarks)
            if (detectedLandmarks.isEmpty()) { stabilizer.clear(); return }
            val now = System.currentTimeMillis()
            if (now - lastPredictionTime < predictionCooldown.value) return
            if (!stabilizer.shouldSample(now, frameSampleIntervalMs.value)) return
            val prediction = recognitionManager.recognize(detectedLandmarks, liveConfidenceThreshold.value) ?: return
            stabilizer.add(prediction.letter, now)
            stabilizer.resolve(requiredMatches.value)?.let { onRecognizeLetter(it).also { lastPredictionTime = now } }
        } catch (e: Exception) {
            Log.e("Camera", "Analyzer error", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun onRecognizeLetter(letter: String) {
        runCatching { Letter(letter, getAslDrawable(getApplication(), letter)) }
            .onSuccess { uiState = uiState.copy(recognizedText = uiState.recognizedText + it) }
    }

    private fun processPhoto(uri: Uri) {
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val bitmap = loadBitmapFromUri(getApplication(), uri) ?: return@launch
            processBitmapPipeline(bitmap, updateLandmarks = true)
        }
    }

    private fun processVideo(uri: Uri) {
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                videoExtractor.forEachFrame(uri, videoFrameSampleIntervalMs.value) { bitmap -> processBitmapPipeline(bitmap, false) }
            }.onFailure { Log.e("Media", "Failed to process video frames", it) }
        }
    }

    private fun processBitmapPipeline(bitmap: Bitmap, updateLandmarks: Boolean) {
        val primary = recognitionManager.recognize(bitmap, photoConfidenceThreshold.value)
        if (updateLandmarks) uiState = uiState.copy(landmarks = primary.landmarks)
        val prediction = primary.result ?: recognitionManager.recognize(primary.landmarks, videoConfidenceThreshold.value)
        prediction?.let { onRecognizeLetter(it.letter) }
    }

    fun onRecognizeSingleVideoFrame(uri: Uri, positionMs: Long) {
        mediaProcessingJob?.cancel()
        uiState = uiState.copy(isSingleFrameRecognizing = true, singleFrameRecognitionFailed = false, singleFrameRecognitionResult = null)
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val predicted = withTimeoutOrNull(singleFrameRecognitionTimeoutMs.value) {
                val frame = videoExtractor.extractSingleFrame(uri, positionMs) ?: return@withTimeoutOrNull null
                recognitionManager.recognize(frame, photoConfidenceThreshold.value).result
                    ?: recognitionManager.recognize(frame, videoConfidenceThreshold.value).result
            }
            withContext(Dispatchers.Main) {
                uiState = if (predicted == null) uiState.copy(isSingleFrameRecognizing = false, singleFrameRecognitionFailed = true)
                else uiState.copy(
                    isSingleFrameRecognizing = false,
                    singleFrameRecognitionResult = Letter(predicted.letter, getAslDrawable(getApplication(), predicted.letter))
                )
            }
        }
    }

    fun dismissSingleFrameRecognitionResult() {
        uiState = uiState.copy(singleFrameRecognitionResult = null, singleFrameRecognitionFailed = false, isSingleFrameRecognizing = false)
    }

    private fun clearCameraFlow() { stopCamera(); mediaProcessingJob?.cancel(); uiState = uiState.copy(landmarks = emptyList()); stabilizer.clear() }
    private fun resolveMediaType(uri: Uri): SelectedMediaType { val mime = getApplication<Application>().contentResolver.getType(uri).orEmpty(); return when { mime.startsWith("image/") -> SelectedMediaType.PHOTO; mime.startsWith("video/") -> SelectedMediaType.VIDEO; else -> SelectedMediaType.NONE } }

    override fun onCleared() {
        super.onCleared()
        clearCameraFlow()
        cameraExecutor.shutdown()
        classifier.close()
    }
}
