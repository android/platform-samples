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

package com.example.platform.camerax

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXBasic() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to control whether to show camera preview or captured image
    var showCapturedImage by remember { mutableStateOf<Uri?>(null) }

    // Request camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Executor for camera operations
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // CameraX use cases
    val previewUseCase = remember { Preview.Builder().build() }
    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }

    // Get CameraProvider asynchronously
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    // Effect to get the CameraProvider once available
    LaunchedEffect(cameraProviderFuture) {
        cameraProvider = suspendCoroutine { continuation ->
            cameraProviderFuture.addListener(
                {
                    continuation.resume(cameraProviderFuture.get())
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    // Effect to bind use cases when cameraProvider is available and permission is granted
    LaunchedEffect(cameraProvider, cameraPermissionState.status) {
        val provider = cameraProvider
        if (provider != null && cameraPermissionState.status == PermissionStatus.Granted) {
            try {
                // Unbind all use cases before rebinding
                provider.unbindAll()

                // Select camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Bind use cases to the lifecycle
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    imageCaptureUseCase,
                )
            } catch (exc: Exception) {
                Log.e("CameraXComposeApp", "Use case binding failed", exc)
                Toast.makeText(context, "Failed to initialize camera.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraPermissionState.status) {
            is PermissionStatus.Granted -> {
                if (showCapturedImage != null) {
                    // Show the captured image with a back button
                    CapturedImageView(uri = showCapturedImage!!) {
                        showCapturedImage = null // Return to preview on back button click
                    }
                } else {
                    // Show camera preview
                    CameraPreview(previewUseCase)
                }
            }

            is PermissionStatus.Denied -> {
                // Show permission request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
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
        }

        // Capture button (only visible when showing preview)
        if (showCapturedImage == null && cameraPermissionState.status == PermissionStatus.Granted) {
            Button(
                onClick = {
                    takePhoto(
                        context = context,
                        imageCapture = imageCaptureUseCase,
                        executor = cameraExecutor,
                        onImageCaptured = { uri -> showCapturedImage = uri },
                        onError = { exc ->
                            ContextCompat.getMainExecutor(context).execute {
                                Toast.makeText(
                                    context,
                                    "Photo capture failed: ${exc.message}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            Log.e("CameraXComposeApp", "Photo capture failed: ${exc.message}", exc)
                        },
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Text("Take Photo")
            }
        }
    }
}

@Composable
fun CameraPreview(previewUseCase: Preview) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                // Bind the Preview use case to the PreviewView's surfaceProvider
                previewUseCase.surfaceProvider = this.surfaceProvider
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun CapturedImageView(uri: Uri, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Captured Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit, // Or your preferred scaling
        )

        // Back button
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

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onImageCaptured: (Uri?) -> Unit,
    onError: (ImageCaptureException) -> Unit,
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", java.util.Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXBasic")
        }
    }

    // Create output options object for saving the file in MediaStore
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        )
        .build()

    // Set up image capture listener, which is triggered after the photo has been taken
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                onError(exc)
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(
                        context,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onImageCaptured(output.savedUri)
                val msg = "Photo capture succeeded: ${output.savedUri}"
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                Log.d("CameraXComposeApp", msg)
            }
        },
    )
}