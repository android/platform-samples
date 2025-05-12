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

package com.example.platform.camerax

import android.Manifest
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.platform.camerax.viewmodels.CameraExtensionsState
import com.example.platform.camerax.viewmodels.CameraExtensionsViewModel
import com.example.platform.camerax.viewmodels.CameraExtensionsViewModel.Companion.extensionModeToString
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXExtensions(
    viewModel: CameraExtensionsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Request camera permission using Accompanist
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // --- Initialization ---
    // Initialize the ViewModel when permission is granted
    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status == PermissionStatus.Granted) {
            viewModel.initialize(context)
        }
    }

    // --- Display Rotation Listener ---
    // Use WindowManager for API levels below 30 to get the display rotation
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var currentRotation by remember { mutableIntStateOf(windowManager.defaultDisplay.rotation) }
    DisposableEffect(context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                // Use WindowManager for API levels below 30
                @Suppress("DEPRECATION")
                val newRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display.rotation
                } else {
                    windowManager.defaultDisplay.rotation
                }

                if (newRotation != currentRotation) {
                    Log.d("CameraExtScreen", "Rotation changed: $newRotation")
                    currentRotation = newRotation
                    viewModel.updateTargetRotation(newRotation) // Inform ViewModel
                }
            }
        }
        displayManager.registerDisplayListener(displayListener, null)
        onDispose { displayManager.unregisterDisplayListener(displayListener) }
    }

    // --- UI Structure ---
    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraPermissionState.status) {
            PermissionStatus.Granted -> {
                // Permission is granted, show the camera view
                CameraView(
                    viewModel = viewModel,
                    uiState = uiState,
                    lifecycleOwner = lifecycleOwner,
                    targetRotation = currentRotation,
                )
            }

            is PermissionStatus.Denied -> {
                // Permission is denied, show a message and a button to request permission
                PermissionRequestScreen(
                    status = cameraPermissionState.status,
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                )
            }
        }
    }
}


// --- UI Components ---

@Composable
private fun CameraView(
    viewModel: CameraExtensionsViewModel,
    uiState: CameraExtensionsState,
    lifecycleOwner: LifecycleOwner,
    targetRotation: Int,
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    // Effect to bind use cases when permission, lens, extension, or rotation changes
    LaunchedEffect(
        viewModel.cameraProvider, // Add dependency on cameraProvider
        viewModel.extensionsManager, // Add dependency on extensionsManager
        uiState.lensFacing,
        uiState.selectedExtension,
        targetRotation, // Rebind if rotation changes significantly for targetRotation setting
    ) {
        // Trigger binding once cameraProvider and extensionsManager are ready
        if (viewModel.cameraProvider != null && viewModel.extensionsManager != null && uiState.errorMessage == null) {
            Log.d(
                "CameraExtScreen",
                "Triggering bindUseCases: Lens=${uiState.lensFacing}, Ext=${
                    extensionModeToString(uiState.selectedExtension)
                }, Rot=$targetRotation",
            )
            viewModel.bindUseCases(
                context = context,
                lifecycleOwner = lifecycleOwner,
                surfaceProvider = previewView.surfaceProvider,
                targetRotation = targetRotation,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = {
                previewView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER // Adjust as needed
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            modifier = Modifier.fillMaxSize(),
            // No update block needed here as LaunchedEffect handles rebinding
        )

        // Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Extension Selection
            ExtensionSelector(
                availableExtensions = uiState.availableExtensions[uiState.lensFacing] ?: listOf(),
                selectedExtension = uiState.selectedExtension,
                onExtensionSelected = { viewModel.selectExtension(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Bottom Row: Capture and Switch Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Add padding for navigation bar
                    .padding(bottom = 20.dp), // Extra padding from bottom
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround, // Space out buttons
            ) {
                // Placeholder for gallery button if needed
                Spacer(modifier = Modifier.size(60.dp))

                // Capture Button
                CaptureButton(
                    isTakingPicture = uiState.isTakingPicture,
                    onClick = { viewModel.takePicture(context) },
                )

                // Switch Camera Button
                SwitchCameraButton(
                    availableExtensions = uiState.availableExtensions,
                    currentLensFacing = uiState.lensFacing,
                    onClick = { viewModel.switchCamera() },
                )
            }
        }
    }

    // Handle Loading and Error states within the CameraView if permission is granted
    when {
        uiState.isLoading -> LoadingScreen()
        uiState.errorMessage != null -> ErrorScreen(message = uiState.errorMessage)
    }
}

@Composable
fun ExtensionSelector(
    availableExtensions: List<Int>,
    selectedExtension: Int,
    onExtensionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableExtensions.size <= 1) { // Only show if there's more than NONE
        return
    }

    Row(
        modifier = modifier
            .statusBarsPadding() // Add padding for status bar
            .horizontalScroll(rememberScrollState())
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        availableExtensions.forEach { mode ->
            val isSelected = mode == selectedExtension
            Text(
                text = extensionModeToString(mode),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onExtensionSelected(mode) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}


@Composable
fun CaptureButton(isTakingPicture: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = !isTakingPicture,
        modifier = Modifier
            .size(72.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(4.dp) // Padding inside the border
            .background(Color.White.copy(alpha = 0.3f), CircleShape),

        ) {
        if (isTakingPicture) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp), // Slightly smaller than button
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
            )
        }
    }
}

@Composable
fun SwitchCameraButton(
    availableExtensions: Map<Int, List<Int>>,
    currentLensFacing: Int,
    onClick: () -> Unit,
) {
    val otherLens = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
        CameraSelector.LENS_FACING_FRONT
    } else {
        CameraSelector.LENS_FACING_BACK
    }
    // Enable switch if the other lens exists in the available extensions map
    val isEnabled = availableExtensions.containsKey(otherLens)

    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(60.dp),
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = "Switch camera",
            tint = if (isEnabled) Color.White else Color.Gray,
            modifier = Modifier.size(36.dp),
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    status: PermissionStatus,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Clear,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))

        val textToShow = if (status.shouldShowRationale) {
            "The camera is important for this feature. Please grant the permission."
        } else {
            "Camera permission is required for this feature to be available. " +
                    "Please grant the permission"
        }

        Text(
            textToShow,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Please grant the permission to continue.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Initializing Camera...", color = Color.White)
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Clear,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Camera Error",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}