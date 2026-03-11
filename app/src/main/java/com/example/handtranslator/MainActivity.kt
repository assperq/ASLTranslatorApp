package com.example.handtranslator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.handtranslator.translator.InputMode
import com.example.handtranslator.translator.MainScreen
import com.example.handtranslator.ui.theme.HandTranslatorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private var hasCameraPermission by mutableStateOf(false)

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            hasCameraPermission = permissions.entries
                .filter { it.key in requiredPermissions }
                .all { it.value }

            if (!hasCameraPermission) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onCameraPermissionGranted(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasCameraPermission = isCameraPermissionGranted()

        setContent {
            HandTranslatorTheme {
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        MainScreen(
                            inputMode = viewModel.inputMode,
                            onInputModeChange = {
                                viewModel.onInputModeChange(it, this, hasCameraPermission)
                                if (it == InputMode.CAMERA && !hasCameraPermission) {
                                    ensureCameraPermission()
                                }
                            },
                            showLandmarks = viewModel.showLandmarks,
                            onShowLandmarksChange = viewModel::onShowLandmarksChange,
                            cameraFacing = viewModel.cameraFacing,
                            onCameraFacingChange = {
                                viewModel.onCameraFacingChange(it, this)
                            },
                            recognizedText = viewModel.recognizedText,
                            textInput = viewModel.textInput,
                            onTextInputChange = viewModel::onTextInputChange,
                            landmarks = viewModel.landmarks,
                            onPreviewViewReady = { view ->
                                viewModel.onPreviewViewReady(view, this, hasCameraPermission)
                                if (!hasCameraPermission) {
                                    ensureCameraPermission()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun ensureCameraPermission() {
        if (isCameraPermissionGranted()) {
            hasCameraPermission = true
            viewModel.onCameraPermissionGranted(this)
        } else {
            activityResultLauncher.launch(requiredPermissions)
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopCamera()
    }
}
