/*
 * Copyright 2023 The Android Open Source Project
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
package com.example.platform.camerax.extensions

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Helper data class for UI state
data class CameraExtensionsState(
    val cameraPermissionGranted: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val availableExtensions: Map<Int, List<Int>> = emptyMap(), // Map<LensFacing, List<ExtensionMode>>
    val selectedExtension: Int = ExtensionMode.NONE,
    val isTakingPicture: Boolean = false,
    val lastCapturedUri: Uri? = null, // Optional: for showing thumbnail
)

class CameraExtensionsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraExtensionsState())
    val uiState: StateFlow<CameraExtensionsState> = _uiState.asStateFlow()

    internal var cameraProvider: ProcessCameraProvider? = null
    internal var extensionsManager: ExtensionsManager? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: Camera? = null

    /** Executor for background camera operations */
    private lateinit var cameraExecutor: ExecutorService

    // Deferred objects to wait for async initialization
    private var cameraProviderDeferred = CompletableDeferred<ProcessCameraProvider>()
    private var extensionsManagerDeferred = CompletableDeferred<ExtensionsManager>()

    // --- Initialization and Setup ---

    fun initialize(context: Context) {
        if (this::cameraExecutor.isInitialized) return // Avoid re-initialization

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        cameraExecutor = Executors.newSingleThreadExecutor()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize CameraProvider and ExtensionsManager concurrently
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        try {
                            cameraProvider = providerFuture.get()
                            cameraProviderDeferred.complete(cameraProvider!!)

                            // Now initialize ExtensionsManager after getting provider
                            val extensionsFuture =
                                ExtensionsManager.getInstanceAsync(context, cameraProvider!!)
                            extensionsFuture.addListener(
                                {
                                    try {
                                        extensionsManager = extensionsFuture.get()
                                        extensionsManagerDeferred.complete(extensionsManager!!)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to initialize ExtensionsManager", e)
                                        extensionsManagerDeferred.completeExceptionally(e)
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                errorMessage = "Failed to initialize Camera Extensions: ${e.localizedMessage}",
                                            )
                                        }
                                    }
                                },
                                ContextCompat.getMainExecutor(context),
                            )

                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to initialize CameraProvider", e)
                            cameraProviderDeferred.completeExceptionally(e)
                            extensionsManagerDeferred.completeExceptionally(e) // Fail extensions too
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to initialize Camera Provider: ${e.localizedMessage}",
                                )
                            }
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )

                // Wait for both to complete
                cameraProvider = cameraProviderDeferred.await()
                extensionsManager = extensionsManagerDeferred.await()

                // Check available extensions after initialization
                checkAvailableExtensions()
                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Camera initialization failed: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    private fun checkAvailableExtensions() {
        val provider = cameraProvider ?: return
        val manager = extensionsManager ?: return
        val allExtensionModes = listOf(
            ExtensionMode.BOKEH,
            ExtensionMode.HDR,
            ExtensionMode.NIGHT,
            ExtensionMode.FACE_RETOUCH,
            ExtensionMode.AUTO,
        )

        val available: MutableMap<Int, List<Int>> = mutableMapOf()

        // Check for Back Camera
        val backCameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        if (provider.hasCamera(backCameraSelector)) {
            val backExtensions = allExtensionModes.filter { manager.isExtensionAvailable(backCameraSelector, it) }
            available[CameraSelector.LENS_FACING_BACK] = listOf(ExtensionMode.NONE) + backExtensions
        } else {
            available[CameraSelector.LENS_FACING_BACK] = listOf(ExtensionMode.NONE)
        }

        // Check for Front Camera
        val frontCameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        if (provider.hasCamera(frontCameraSelector)) {
            val frontExtensions = allExtensionModes
                .filter { manager.isExtensionAvailable(frontCameraSelector, it) }
            available[CameraSelector.LENS_FACING_FRONT] =
                listOf(ExtensionMode.NONE) + frontExtensions
        } else {
            available[CameraSelector.LENS_FACING_FRONT] = listOf(ExtensionMode.NONE)
        }

        Log.d(TAG, "Available extensions: $available")
        _uiState.update {
            val currentLensExtensions = available[it.lensFacing] ?: listOf(ExtensionMode.NONE)
            // Reset selected extension if it's not available for the current lens
            val newSelectedExtension = if (currentLensExtensions.contains(it.selectedExtension)) {
                it.selectedExtension
            } else {
                ExtensionMode.NONE
            }
            it.copy(availableExtensions = available, selectedExtension = newSelectedExtension)
        }
    }

    // --- Camera Binding ---

    fun bindUseCases(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        targetRotation: Int,
    ) {
        val provider = cameraProvider ?: run { Log.e(TAG, "CameraProvider not ready"); return }
        val manager = extensionsManager ?: run { Log.e(TAG, "ExtensionsManager not ready"); return }
        val lensFacing = _uiState.value.lensFacing
        val selectedExtension = _uiState.value.selectedExtension

        viewModelScope.launch(Dispatchers.Main) { // Ensure binding happens on the main thread
            try {
                // 1. Create CameraSelector (base or extension-enabled)
                val baseCameraSelector =
                    CameraSelector.Builder().requireLensFacing(lensFacing).build()

                val cameraSelector = if (selectedExtension != ExtensionMode.NONE &&
                    manager.isExtensionAvailable(baseCameraSelector, selectedExtension)
                ) {
                    Log.d(
                        TAG,
                        "Binding with extension: ${extensionModeToString(selectedExtension)}",
                    )
                    manager.getExtensionEnabledCameraSelector(baseCameraSelector, selectedExtension)
                } else {
                    Log.d(
                        TAG,
                        "Binding without extension (Mode: ${extensionModeToString(selectedExtension)})",
                    )
                    baseCameraSelector
                }

                // 2. Build Use Cases (Preview and ImageCapture)
                // Aspect ratio can be determined more dynamically if needed
                val aspectRatio = AspectRatio.RATIO_16_9 // Or AspectRatio.RATIO_4_3

                preview = Preview.Builder()
                    .setTargetRotation(targetRotation)
                    .setTargetAspectRatio(aspectRatio)
                    .build()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(targetRotation)
                    .setTargetAspectRatio(aspectRatio)
                    .build()

                // 3. Unbind existing use cases before rebinding
                provider.unbindAll()

                // 4. Bind new use cases
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )

                // 5. Attach SurfaceProvider
                preview?.setSurfaceProvider(surfaceProvider)
                Log.d(
                    TAG,
                    "Use cases bound successfully for lens $lensFacing, extension ${
                        extensionModeToString(selectedExtension)
                    }",
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                _uiState.update { it.copy(errorMessage = "Failed to bind camera: ${exc.localizedMessage}") }
                // Attempt to fallback to NONE mode if extension binding failed?
                if (selectedExtension != ExtensionMode.NONE) {
                    Log.w(TAG, "Falling back to ExtensionMode.NONE")
                    selectExtension(ExtensionMode.NONE)
                    // Recursive call might be risky, maybe just signal UI to retry/reset?
                }
            }
        }
    }

    // --- User Actions ---

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
        if (!granted) {
            _uiState.update {
                it.copy(
                    errorMessage = "Camera permission is required.",
                    isLoading = false,
                )
            }
        }
        // Initialization might depend on permission, trigger if needed,
        // but `initialize` is usually called once from the Composable's LaunchedEffect.
        // If permission is granted later, the Composable's effect should re-trigger binding.
    }

    fun switchCamera() {
        val currentLensFacing = _uiState.value.lensFacing
        val newLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        // Check if the new lens facing has any available extensions (including NONE)
        if (_uiState.value.availableExtensions[newLensFacing]?.isNotEmpty() == true) {
            Log.d(TAG, "Switching camera to $newLensFacing")
            // Reset selected extension if it's not supported by the new lens
            val newLensExtensions =
                _uiState.value.availableExtensions[newLensFacing] ?: listOf(ExtensionMode.NONE)
            val newSelectedExtension =
                if (newLensExtensions.contains(_uiState.value.selectedExtension)) {
                    _uiState.value.selectedExtension
                } else {
                    ExtensionMode.NONE // Default to NONE if current extension not supported
                }
            _uiState.update {
                it.copy(
                    lensFacing = newLensFacing,
                    selectedExtension = newSelectedExtension,
                )
            }
            // Rebinding will be triggered by the Composable observing these state changes
        } else {
            Log.w(
                TAG,
                "Cannot switch camera: Lens facing $newLensFacing not available or has no modes.",
            )
            _uiState.update { it.copy(errorMessage = "Cannot switch to other camera.") }
        }
    }

    fun selectExtension(extensionMode: Int) {
        val currentLens = _uiState.value.lensFacing
        val availableForLens = _uiState.value.availableExtensions[currentLens] ?: listOf()

        if (availableForLens.contains(extensionMode)) {
            if (_uiState.value.selectedExtension != extensionMode) {
                Log.d(TAG, "Selecting extension: ${extensionModeToString(extensionMode)}")
                _uiState.update { it.copy(selectedExtension = extensionMode) }
                // Rebinding will be triggered by the Composable observing this state change
            }
        } else {
            Log.w(
                TAG,
                "Extension ${extensionModeToString(extensionMode)} not available for lens $currentLens",
            )
        }
    }

    fun takePicture(context: Context) {
        val imageCapture = this.imageCapture ?: run {
            _uiState.update { it.copy(errorMessage = "Camera not ready for capture.") }
            return
        }
        if (_uiState.value.isTakingPicture) return // Prevent multiple captures

        _uiState.update { it.copy(isTakingPicture = true, errorMessage = null) }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                val appName = "CameraXExtensions" // Ensure this string exists
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$appName")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues,
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context), // Callback on main thread
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    _uiState.update {
                        it.copy(
                            isTakingPicture = false,
                            errorMessage = "Capture failed: ${exc.message} (Code: ${exc.imageCaptureError})",
                        )
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    _uiState.update { it.copy(isTakingPicture = false, lastCapturedUri = savedUri) }

                    // Show a Toast with the saved image location
                    Toast.makeText(
                        context,
                        "Photo saved to: $savedUri",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Optionally trigger flash animation or sound here via state update
                }
            },
        )
    }

    fun updateTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        preview?.targetRotation = rotation
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Cleanup ---

    override fun onCleared() {
        super.onCleared()
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera provider on clear", e)
        }
        if (this::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
        Log.d(TAG, "ViewModel cleared and resources released.")
    }

    companion object {
        private const val TAG = "CameraExtViewModel"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"

        // Helper to convert ExtensionMode Int to String for logging/display
        fun extensionModeToString(mode: Int): String {
            return when (mode) {
                ExtensionMode.NONE -> "NONE"
                ExtensionMode.BOKEH -> "BOKEH"
                ExtensionMode.HDR -> "HDR"
                ExtensionMode.NIGHT -> "NIGHT"
                ExtensionMode.FACE_RETOUCH -> "FACE_RETOUCH"
                ExtensionMode.AUTO -> "AUTO"
                else -> "UNKNOWN ($mode)"
            }
        }
    }
}