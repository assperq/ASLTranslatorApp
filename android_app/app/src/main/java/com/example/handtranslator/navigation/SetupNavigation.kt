package com.example.handtranslator.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.handtranslator.preferences.PreferencesScreen
import com.example.handtranslator.translator.InputMode
import com.example.handtranslator.translator.MainScreen
import com.example.handtranslator.translator.TranslatorViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupNavigation(
    navHostController: NavHostController,
    hasCameraPermission : Boolean,
    ensureCameraPermission : () -> Unit,
    lifecycleOwner: LifecycleOwner,
    translatorViewModel: TranslatorViewModel = koinViewModel(),
) {
    NavHost(navHostController,
        Routes.MainScreen.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(Routes.MainScreen.route) {
            Scaffold { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    MainScreen(
                        inputMode = translatorViewModel.inputMode,
                        onInputModeChange = {
                            translatorViewModel.onInputModeChange(it, lifecycleOwner , hasCameraPermission)
                            if (it == InputMode.CAMERA && !hasCameraPermission) {
                                ensureCameraPermission()
                            }
                        },
                        showLandmarks = translatorViewModel.showLandmarks,
                        onShowLandmarksChange = translatorViewModel::onShowLandmarksChange,
                        cameraFacing = translatorViewModel.cameraFacing,
                        onCameraFacingChange = {
                            translatorViewModel.onCameraFacingChange(it, lifecycleOwner)
                        },
                        recognizedText = translatorViewModel.recognizedText,
                        textInput = translatorViewModel.textInput,
                        onTextInputChange = translatorViewModel::onTextInputChange,
                        landmarks = translatorViewModel.landmarks,
                        onPreviewViewReady = { view ->
                            translatorViewModel.onPreviewViewReady(view, lifecycleOwner, hasCameraPermission)
                            if (!hasCameraPermission) {
                                ensureCameraPermission()
                            }
                        },
                        onTorchEnabledChange = translatorViewModel::onTorchEnabledChange,
                        isTorchEnabled = translatorViewModel.isTorchEnabled,
                        isTorchSupported = translatorViewModel.isTorchSupported,
                        onClearRecognizedText = translatorViewModel::onClearRecognizedText,
                        cameraContentMode = translatorViewModel.cameraContentMode,
                        selectedMediaUri = translatorViewModel.selectedMediaUri,
                        selectedMediaType = translatorViewModel.selectedMediaType,
                        onSelectMedia = translatorViewModel::onSelectMedia,
                        onSwitchToCameraPreview = {
                            translatorViewModel.onSwitchToCameraPreview(lifecycleOwner, hasCameraPermission)
                        },
                        onOpenSettings = {
                            navHostController.navigate(Routes.Preferences.route)
                        }
                    )
                }
            }
        }

        composable(Routes.Preferences.route) {
            val predictionCooldown by translatorViewModel.predictionCooldown.collectAsState()
            val requiredMatches by translatorViewModel.requiredMatches.collectAsState()
            val frameSampleIntervalMs by translatorViewModel.frameSampleIntervalMs.collectAsState()
            val liveConfidenceThreshold by translatorViewModel.liveConfidenceThreshold.collectAsState()

            Scaffold { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    PreferencesScreen(
                        predictionCooldown = predictionCooldown,
                        requiredMatches = requiredMatches,
                        frameSampleIntervalMs = frameSampleIntervalMs,
                        liveConfidenceThreshold = liveConfidenceThreshold,
                        onPredictionCooldownChange = translatorViewModel::setPredictionCooldown,
                        onRequiredMatchesChange = translatorViewModel::setRequiredMatches,
                        onFrameSampleIntervalMsChange = translatorViewModel::setFrameSampleIntervalMs,
                        onLiveConfidenceThresholdChange = translatorViewModel::setLiveConfidenceThreshold,
                        onBackClick = {
                            navHostController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
