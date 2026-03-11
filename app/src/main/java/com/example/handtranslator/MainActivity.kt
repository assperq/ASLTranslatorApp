package com.example.handtranslator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.handtranslator.ui.theme.HandTranslatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.security.Permissions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import com.example.handtranslator.Helper.landmarksTo210Features
import com.example.handtranslator.Helper.loadAslLabels
import com.example.handtranslator.CameraFacing
import com.example.handtranslator.MainScreen
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private lateinit var aslClassifier: AslClassifier
    private var lastPredictionTime = 0L

    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private var inputMode by mutableStateOf(InputMode.CAMERA)
    private var showLandmarks by mutableStateOf(false)
    private var cameraFacing by mutableStateOf(CameraFacing.FRONT)
    private var recognizedText by mutableStateOf("Перевод появится здесь")
    private var textInput by mutableStateOf("")
    private var landmarks by mutableStateOf<List<NormalizedLandmark>>(emptyList())

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val permissionGranted = permissions.entries
                .filter { it.key in requiredPermissions }
                .all { it.value }

            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                bindCameraUseCases()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handLandmarkerHelper = HandLandmarkerHelper(this)
        aslClassifier = AslClassifier(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            HandTranslatorTheme {
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        MainScreen(
                            inputMode = inputMode,
                            onInputModeChange = {
                                inputMode = it
                                if (it == InputMode.CAMERA) {
                                    ensureCameraPermissionAndBind()
                                } else {
                                    cameraProvider?.unbindAll()
                                    landmarks = emptyList()
                                }
                            },
                            showLandmarks = showLandmarks,
                            onShowLandmarksChange = { showLandmarks = it },
                            cameraFacing = cameraFacing,
                            onCameraFacingChange = {
                                cameraFacing = it
                                if (inputMode == InputMode.CAMERA) {
                                    bindCameraUseCases()
                                }
                            },
                            recognizedText = recognizedText,
                            textInput = textInput,
                            onTextInputChange = {
                                textInput = it
                                recognizedText = it
                            },
                            landmarks = landmarks,
                            onPreviewViewReady = { view ->
                                previewView = view
                                if (inputMode == InputMode.CAMERA) {
                                    ensureCameraPermissionAndBind()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun ensureCameraPermissionAndBind() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(requiredPermissions)
        } else {
            bindCameraUseCases()
        }
    }

    private fun bindCameraUseCases() {
        val currentPreviewView = previewView ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

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
            cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        val detectedLandmarks = handLandmarkerHelper.detect(imageProxy)
        runOnUiThread {
            landmarks = detectedLandmarks.orEmpty()
        }

        if (!detectedLandmarks.isNullOrEmpty()) {
            processFrameWithPrediction(detectedLandmarks)
        }
    }


    fun processFrameWithPrediction(landmarks: List<NormalizedLandmark>) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPredictionTime < 1500) return // ждем 1.5 сек

        lastPredictionTime = currentTime

        CoroutineScope(Dispatchers.Default).launch {
            val features = landmarksTo210Features(landmarks)
            val predictedIndex = aslClassifier.predict(features)
            val aslLabels = loadAslLabels(this@MainActivity)

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

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}