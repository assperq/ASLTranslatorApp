package com.example.handtranslator.translator

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
import androidx.lifecycle.viewModelScope
import com.example.handtranslator.AslClassifier
import com.example.handtranslator.HandLandmarkerHelper
import com.example.handtranslator.Helper.getAslDrawable
import com.example.handtranslator.Helper.loadAslLabels
import com.example.handtranslator.Helper.loadBitmapFromUri
import com.example.handtranslator.data.preferences.DataStoreManager
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
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

    private val dataStoreManager = DataStoreManager(application)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val classifier = AslClassifier(application.applicationContext)
    private val labels by lazy { loadAslLabels(application.applicationContext) }
    private val recognitionManager = RecognitionManager(
        handLandmarker = HandLandmarkerHelper(application.applicationContext),
        engine = GestureRecognitionEngine(classifier, labels)
    )
    private val stabilizer = PredictionStabilizer(SLIDING_WINDOW_SIZE)
    private val videoExtractor = VideoFrameExtractor(application)

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeCamera: Camera? = null
    private var previewView: PreviewView? = null
    private var lastPredictionTime = 0L
    private var mediaProcessingJob: Job? = null

    val predictionCooldown = dataStoreManager.getPredictionCooldown().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 400L)
    fun setPredictionCooldown(v: Long) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setPredictionCooldown(v) }
    val requiredMatches = dataStoreManager.getRequiredMatches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)
    fun setRequiredMatches(v: Int) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setRequiredMatches(v) }
    val frameSampleIntervalMs = dataStoreManager.getFrameSampleInterval().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90L)
    fun setFrameSampleIntervalMs(v: Long) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setFrameSampleInterval(v) }
    val liveConfidenceThreshold = dataStoreManager.getLiveConfidenceThreshold().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.45f)
    fun setLiveConfidenceThreshold(v: Float) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setLiveConfidenceThreshold(v) }
    val photoConfidenceThreshold = dataStoreManager.getPhotoConfidenceThreshold().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.35f)
    fun setPhotoConfidenceThreshold(v: Float) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setPhotoConfidenceThreshold(v) }
    val videoConfidenceThreshold = dataStoreManager.getVideoConfidenceThreshold().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.2f)
    fun setVideoConfidenceThreshold(v: Float) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setVideoConfidenceThreshold(v) }
    val videoFrameSampleIntervalMs = dataStoreManager.getVideoFrameSampleInterval().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1500L)
    fun setVideoFrameSampleIntervalMs(v: Long) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setVideoFrameSampleInterval(v) }
    val videoPreviewFillEnabled = dataStoreManager.getVideoPreviewFillEnabled().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setVideoPreviewFillEnabled(v: Boolean) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setVideoPreviewFillEnabled(v) }
    val singleFrameRecognitionTimeoutMs = dataStoreManager.getSingleFrameRecognitionTimeoutMs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2500L)
    fun setSingleFrameRecognitionTimeoutMs(v: Long) = viewModelScope.launch(Dispatchers.IO) { dataStoreManager.setSingleFrameRecognitionTimeoutMs(v) }

    var isTorchSupported by mutableStateOf(false); private set
    var isTorchEnabled by mutableStateOf(false); private set
    var inputMode by mutableStateOf(InputMode.CAMERA); private set
    var cameraContentMode by mutableStateOf(CameraContentMode.LIVE_CAMERA); private set
    var selectedMediaUri by mutableStateOf<Uri?>(null); private set
    var selectedMediaType by mutableStateOf(SelectedMediaType.NONE); private set
    var showLandmarks by mutableStateOf(false); private set
    var cameraFacing by mutableStateOf(CameraFacing.FRONT); private set
    var recognizedText by mutableStateOf(emptyList<Letter>()); private set
    var textInput by mutableStateOf(""); private set
    var landmarks by mutableStateOf<List<NormalizedLandmark>>(emptyList()); private set
    var singleFrameRecognitionResult by mutableStateOf<Letter?>(null); private set
    var isSingleFrameRecognizing by mutableStateOf(false); private set
    var singleFrameRecognitionFailed by mutableStateOf(false); private set

    fun onInputModeChange(mode: InputMode, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) { inputMode = mode; if (mode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA && hasCameraPermission) bindCameraUseCases(lifecycleOwner) else clearCameraFlow() }
    fun onClearRecognizedText(oneLetter: Boolean) { recognizedText = if (oneLetter) recognizedText.dropLast(1) else emptyList() }
    fun onRecognizeLetter(letter: String) { runCatching { Letter(name = letter, imageCard = getAslDrawable(getApplication(), letter)) }.onSuccess { recognizedText = recognizedText + it } }
    fun onTorchEnabledChange(enabled: Boolean) { isTorchEnabled = enabled; activeCamera?.cameraControl?.enableTorch(enabled) }
    fun onShowLandmarksChange(show: Boolean) { showLandmarks = show }
    fun onCameraFacingChange(facing: CameraFacing, lifecycleOwner: LifecycleOwner) { cameraFacing = facing; stabilizer.clear(); if (inputMode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA) bindCameraUseCases(lifecycleOwner) }
    fun onTextInputChange(text: String) { textInput = text; recognizedText = text.map { ch -> ch.toString().let { Letter(it, getAslDrawable(getApplication(), it)) } } }
    fun onPreviewViewReady(view: PreviewView, lifecycleOwner: LifecycleOwner, hasCameraPermission: Boolean) { previewView = view; if (inputMode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA && hasCameraPermission) bindCameraUseCases(lifecycleOwner) }
    fun onCameraPermissionGranted(lifecycleOwner: LifecycleOwner) { if (inputMode == InputMode.CAMERA && cameraContentMode == CameraContentMode.LIVE_CAMERA) bindCameraUseCases(lifecycleOwner) }

    fun onSelectMedia(uri: Uri) {
        selectedMediaUri = uri
        selectedMediaType = resolveMediaType(uri)
        cameraContentMode = CameraContentMode.SELECTED_MEDIA
        clearCameraFlow()
        recognizedText = emptyList()
        mediaProcessingJob?.cancel()
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
        stabilizer.clear()
        if (inputMode == InputMode.CAMERA && hasCameraPermission) bindCameraUseCases(lifecycleOwner)
    }

    fun stopCamera() { cameraProvider?.unbindAll(); activeCamera = null; isTorchSupported = false; isTorchEnabled = false; stabilizer.clear() }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val pv = previewView ?: return
        val future = ProcessCameraProvider.getInstance(getApplication())
        future.addListener({
            cameraProvider = future.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = pv.surfaceProvider }
            val analyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { proxy -> processFrameSafely(proxy) }
            }
            val selector = if (cameraFacing == CameraFacing.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider?.unbindAll()
            activeCamera = cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
            isTorchSupported = activeCamera?.cameraInfo?.hasFlashUnit() == true
            if (!isTorchSupported) isTorchEnabled = false
            activeCamera?.cameraControl?.enableTorch(isTorchEnabled && isTorchSupported)
        }, androidx.core.content.ContextCompat.getMainExecutor(getApplication()))
    }

    private fun processFrameSafely(imageProxy: ImageProxy) {
        try { processFrame(imageProxy) } catch (e: Exception) { Log.e("Camera", "Analyzer error", e) } finally { imageProxy.close() }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val detectedLandmarks = recognitionManager.detect(imageProxy)
        viewModelScope.launch(Dispatchers.Main) { landmarks = detectedLandmarks }
        if (detectedLandmarks.isEmpty()) { stabilizer.clear(); return }

        val now = System.currentTimeMillis()
        if (now - lastPredictionTime < predictionCooldown.value) return
        if (!stabilizer.shouldSample(now, frameSampleIntervalMs.value)) return

        val result = recognitionManager.recognize(detectedLandmarks, liveConfidenceThreshold.value) ?: return
        stabilizer.add(result.letter, now)
        stabilizer.resolve(requiredMatches.value)?.let {
            lastPredictionTime = now
            viewModelScope.launch(Dispatchers.Main) { onRecognizeLetter(it) }
        }
    }

    private fun processPhoto(uri: Uri) {
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val bitmap = loadBitmapFromUri(getApplication(), uri) ?: return@launch
            processBitmapWithFallback(bitmap, updateLandmarks = true)
        }
    }

    private fun processVideo(uri: Uri) {
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                videoExtractor.forEachFrame(uri, videoFrameSampleIntervalMs.value) { bitmap ->
                    processBitmapWithFallback(bitmap, updateLandmarks = false)
                }
            }.onFailure { Log.e("Media", "Failed to process video frames", it) }
        }
    }

    private fun processBitmapWithFallback(bitmap: android.graphics.Bitmap, updateLandmarks: Boolean) {
        val primary = recognitionManager.recognize(bitmap, photoConfidenceThreshold.value)
        if (updateLandmarks) viewModelScope.launch(Dispatchers.Main) { landmarks = primary.landmarks }
        val prediction = primary.result ?: recognitionManager.recognize(primary.landmarks, videoConfidenceThreshold.value)
        prediction?.let { viewModelScope.launch(Dispatchers.Main) { onRecognizeLetter(it.letter) } }
    }

    fun onRecognizeSingleVideoFrame(uri: Uri, positionMs: Long) {
        mediaProcessingJob?.cancel()
        isSingleFrameRecognizing = true
        singleFrameRecognitionFailed = false
        singleFrameRecognitionResult = null
        mediaProcessingJob = viewModelScope.launch(Dispatchers.Default) {
            val predicted = withTimeoutOrNull(singleFrameRecognitionTimeoutMs.value) {
                val frame = videoExtractor.extractSingleFrame(uri, positionMs) ?: return@withTimeoutOrNull null
                val primary = recognitionManager.recognize(frame, photoConfidenceThreshold.value).result
                primary ?: recognitionManager.recognize(frame, videoConfidenceThreshold.value).result
            }
            withContext(Dispatchers.Main) {
                isSingleFrameRecognizing = false
                if (predicted == null) singleFrameRecognitionFailed = true
                else singleFrameRecognitionResult = Letter(predicted.letter, getAslDrawable(getApplication(), predicted.letter))
            }
        }
    }

    fun dismissSingleFrameRecognitionResult() { singleFrameRecognitionResult = null; singleFrameRecognitionFailed = false; isSingleFrameRecognizing = false }
    private fun clearCameraFlow() { stopCamera(); mediaProcessingJob?.cancel(); landmarks = emptyList(); stabilizer.clear() }

    private fun resolveMediaType(uri: Uri): SelectedMediaType {
        val mimeType = getApplication<Application>().contentResolver.getType(uri).orEmpty()
        return when { mimeType.startsWith("image/") -> SelectedMediaType.PHOTO; mimeType.startsWith("video/") -> SelectedMediaType.VIDEO; else -> SelectedMediaType.NONE }
    }

    override fun onCleared() { super.onCleared(); clearCameraFlow(); cameraExecutor.shutdown(); classifier.close() }
}
