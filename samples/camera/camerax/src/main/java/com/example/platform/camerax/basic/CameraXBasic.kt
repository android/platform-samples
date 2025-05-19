/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.camerax.basic

import android.Manifest
import android.net.Uri
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import java.util.UUID.randomUUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * This is a basic CameraX sample demonstrating how to use CameraX with Compose. It handles camera
 * permissions, displays a camera preview, and captures photos.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXBasic(modifier: Modifier = Modifier) {
    var showCapturedImage by remember { mutableStateOf<Uri?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val imageCaptureCallbackExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val viewModel = remember { CameraXBasicViewModel() }

    DisposableEffect(Unit) {
        onDispose {
            imageCaptureCallbackExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ContentWithPermissionHandling(
            cameraPermissionState = cameraPermissionState,
            showCapturedImage = showCapturedImage,
            onShowCapturedImageChange = { showCapturedImage = it },
            viewModel = viewModel,
            imageCaptureCallbackExecutor = imageCaptureCallbackExecutor,
            modifier = modifier,
        )
    }
}

/**
 * Handles camera permission status and displays content accordingly.
 *
 * If permission is granted, it shows either the camera preview or the captured image.
 * If permission is denied, it displays a message and a button to request permission.
 *
 * @param cameraPermissionState The state of the camera permission.
 * @param showCapturedImage The URI of the captured image to display, if any.
 * @param onShowCapturedImageChange Callback function to update the [showCapturedImage] state.
 * @param viewModel The [CameraXBasicViewModel] for handling camera operations.
 * @param imageCaptureCallbackExecutor The executor service for image capture callbacks.
 * @param modifier Modifier to be applied to the layout.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ContentWithPermissionHandling(
    cameraPermissionState: PermissionState,
    showCapturedImage: Uri?,
    onShowCapturedImageChange: (Uri?) -> Unit,
    viewModel: CameraXBasicViewModel,
    imageCaptureCallbackExecutor: ExecutorService,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    when (cameraPermissionState.status) {
        is PermissionStatus.Granted -> {
            if (showCapturedImage != null) {
                CapturedImageView(uri = showCapturedImage) {
                    onShowCapturedImageChange(null)
                }
            } else {
                CameraPreviewContent(
                    viewModel = viewModel,
                    modifier = modifier,
                    lifecycleOwner = LocalLifecycleOwner.current,
                    onTakePhotoClick = {
                        viewModel.takePhoto(
                            context = context,
                            callbackExecutor = imageCaptureCallbackExecutor,
                            onImageCaptured = { uri -> onShowCapturedImageChange(uri) },
                            onError = { /* Error logging/toast handled within takePhoto */ },
                        )
                    },
                )
            }
        }

        is PermissionStatus.Denied -> CameraPermissionDeniedView(
            cameraPermissionState.status,
            cameraPermissionState,
        )
    }
}

/**
 * Composable function that displays a message when camera permission is denied.
 *
 * It provides an option to request the permission again.
 *
 * @param status The current [PermissionStatus] of the camera permission.
 * @param cameraPermissionState The [PermissionState] for the camera permission, used to request permission again.
 */
@Composable
@OptIn(ExperimentalPermissionsApi::class)
private fun CameraPermissionDeniedView(
    status: PermissionStatus,
    cameraPermissionState: PermissionState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val textToShow = if (status.shouldShowRationale) {
            "The camera is needed to take pictures. Please grant the permission."
        } else {
            "Camera permission is required to use this feature."
        }
        Text(text = textToShow)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
            Text("Request Permission")
        }
    }
}

/**
 * Composable function that displays the camera preview.
 *
 * @param viewModel The [CameraXBasicViewModel] for accessing the camera preview surface request.
 * @param modifier Modifier to be applied to the layout.
 * @param onTakePhotoClick Callback function to be invoked when the "Take Photo" button is clicked.
 * @param lifecycleOwner The lifecycle owner to bind the camera to.
 */
@Composable
private fun CameraPreviewContent(
    viewModel: CameraXBasicViewModel,
    modifier: Modifier = Modifier,
    onTakePhotoClick: () -> Unit,
    lifecycleOwner: LifecycleOwner,
) {
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }

    // State for managing the autofocus indicator
    var autofocusRequest by remember { mutableStateOf(randomUUID() to Offset.Unspecified) }

    // Extracting values from the autofocusRequest state
    val autofocusRequestId = autofocusRequest.first
    val showAutofocusIndicator = autofocusRequest.second.isSpecified
    val autofocusLocation = remember(autofocusRequestId) { autofocusRequest.second }

    // Effect to hide the autofocus indicator after a delay
    if (showAutofocusIndicator) {
        LaunchedEffect(autofocusRequestId) {
            delay(1000)
            // Clear the offset to finish the request and hide the indicator
            autofocusRequest = autofocusRequestId to Offset.Unspecified
        }
    }

    surfaceRequest?.let { request ->
        val coordinateTransformer = remember { MutableCoordinateTransformer() }
        Box(modifier = Modifier.fillMaxSize()) {
            CameraXViewfinder(
                surfaceRequest = request,
                coordinateTransformer = coordinateTransformer,
                modifier = Modifier
                    .fillMaxSize() // Ensure CameraXViewfinder fills the Box
                    .pointerInput(viewModel, coordinateTransformer) {
                        detectTapGestures { tapCoords ->
                            with(coordinateTransformer) {
                                viewModel.tapToFocus(tapCoords.transform())
                            }
                            // Update the state to show the autofocus indicator
                            autofocusRequest = randomUUID() to tapCoords
                        }
                    },
            )

            AnimatedVisibility(
                visible = showAutofocusIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .offset { autofocusLocation.takeOrElse { Offset.Zero }.round() }
                    .offset((-24).dp, (-24).dp), // Adjust offset to center the indicator
            ) {
                Spacer(
                    Modifier
                        .border(2.dp, Color.White, CircleShape)
                        .size(48.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = onTakePhotoClick) { Text("Take Photo") }
            }
        }
    }
}

/**
 * Composable function that displays a captured image.
 *
 * @param uri The URI of the image to display.
 * @param onDismiss Callback function to be invoked when the user dismisses the image view
 *                  (e.g., clicks the back button).
 */
@Composable
fun CapturedImageView(uri: Uri, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Captured Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Camera",
            )
        }
    }
}