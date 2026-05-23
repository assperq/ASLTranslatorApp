package com.example.handtranslator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.example.handtranslator.navigation.SetupNavigation
import com.example.handtranslator.translator.TranslatorViewModel
import com.example.handtranslator.ui.theme.HandTranslatorTheme
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModel()
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private var hasCameraPermission by mutableStateOf(false)

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            hasCameraPermission = permissions.entries
                .filter { it.key in requiredPermissions }
                .all { it.value }

            if (!hasCameraPermission) {
                Toast.makeText(baseContext, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onCameraPermissionGranted(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasCameraPermission = isCameraPermissionGranted()

        setContent {
            HandTranslatorTheme {
                val navHostController = rememberNavController()
                SetupNavigation(
                    navHostController,
                    hasCameraPermission,
                    ::ensureCameraPermission,
                    this@MainActivity as LifecycleOwner,
                    viewModel
                )
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
