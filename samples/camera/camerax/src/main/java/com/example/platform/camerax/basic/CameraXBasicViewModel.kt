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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService

/**
 * ViewModel for the CameraX Basic sample.
 *
 * This ViewModel handles the camera setup, preview display, tap to focus, and photo capture
 * functionality using CameraX. It exposes a [StateFlow] for the camera preview [SurfaceRequest]
 * to be used in a composable.
 */
class CameraXBasicViewModel : ViewModel() {
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null

    private var cameraControl: CameraControl? = null

    /**
     * CameraX Preview use case configuration.
     * Sets up a surface provider that updates the [_surfaceRequest] StateFlow.
     */
    private val previewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
            surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                newSurfaceRequest.resolution.width.toFloat(),
                newSurfaceRequest.resolution.height.toFloat(),
            )
        }
    }

    /**
     * CameraX ImageCapture use case configuration.
     */
    private val imageCaptureUseCase = ImageCapture.Builder().build()

    /**
     * Binds the camera to the lifecycle owner and selected use cases.
     *
     * @param appContext The application context.
     * @param lifecycleOwner The lifecycle owner to which the camera will be bound.
     */
    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(previewUseCase)      // Add Preview UseCase
            .addUseCase(imageCaptureUseCase) // Add Image Capture UseCase
            .build()

        val camera = processCameraProvider.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = DEFAULT_BACK_CAMERA, // Default to the back camera
            useCaseGroup = useCaseGroup,
        )

        // Assign camera control for tap-to-focus functionality.
        cameraControl = camera.cameraControl

        // Cancellation signals we're done with the camera
        try {
            awaitCancellation()
        } finally {
            processCameraProvider.unbindAll()
            cameraControl = null
        }
    }

    /**
     * Initiates tap-to-focus at the given coordinates on the preview surface.
     *
     * @param coordinates The coordinates of the tap relative to the preview surface.
     */
    fun tapToFocus(coordinates: Offset) {
        val point = surfaceMeteringPointFactory?.createPoint(coordinates.x, coordinates.y)
        if (point != null) {
            val meteringAction = FocusMeteringAction.Builder(point).build()
            cameraControl?.startFocusAndMetering(meteringAction)
        }
    }

    /**
     * Takes a photo and saves it to external storage.
     *
     * @param context The application context.
     * @param callbackExecutor The executor to run the image capture callbacks on.
     * @param onImageCaptured Callback invoked when the image is successfully captured and saved.
     * @param onError Callback invoked if an error occurs during image capture.
     */
    fun takePhoto(
        context: Context,
        callbackExecutor: ExecutorService,
        onImageCaptured: (Uri?) -> Unit,
        onError: (ImageCaptureException) -> Unit,
    ) {
        val name = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.US,
        ).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXBasic")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
            .build()

        imageCaptureUseCase.takePicture(
            outputOptions,
            callbackExecutor,
            ImageSavedCallback(context, onImageCaptured, onError),
        )
    }
}

/**
 * Callback for handling image capture results.
 *
 * @param context The application context.
 * @param onImageCaptured Callback invoked when the image is successfully captured and saved.
 * @param onError Callback invoked if an error occurs during image capture.
 */
private class ImageSavedCallback(
    private val context: Context,
    private val onImageCaptured: (Uri?) -> Unit,
    private val onErrorA: (ImageCaptureException) -> Unit,
) : ImageCapture.OnImageSavedCallback {
    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
        val savedUri = output.savedUri
        Log.d("CameraXComposeApp", "Photo capture succeeded: $savedUri")
        onImageCaptured(savedUri)
    }

    override fun onError(exc: ImageCaptureException) {
        Log.e("CameraXComposeApp", "Photo capture failed: ${exc.message}", exc)
        ContextCompat.getMainExecutor(context).execute {
            Toast.makeText(context, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT)
                .show()
        }
        onErrorA(exc)
    }
}