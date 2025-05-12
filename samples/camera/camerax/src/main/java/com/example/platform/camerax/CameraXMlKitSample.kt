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
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.platform.camerax.mlkit.QrCodeDrawable
import com.example.platform.camerax.mlkit.QrCodeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXMlKitScreen() {
    var qrCodeDetected by remember { mutableStateOf(false) }
    var qrCodeContent by remember { mutableStateOf("") }

    // Request camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (cameraPermissionState.status) {
            PermissionStatus.Granted -> {
                // Permission is granted, show the camera preview
                CameraPreview(
                    barcodeScanner,
                    { detected -> qrCodeDetected = detected },
                    { content -> qrCodeContent = content },
                )
            }

            is PermissionStatus.Denied -> {
                // Permission is denied, show a message and a button to request permission
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val textToShow =
                        if ((cameraPermissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                            "The camera is important for this feature. Please grant the permission."
                        } else {
                            "Camera permission is required for this feature to be available. " +
                                    "Please grant the permission"
                        }
                    Text(
                        textToShow,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Request permission")
                    }
                }
            }
        }

        QrCodeText(qrCodeDetected, qrCodeContent)
    }
}

@Composable
fun QrCodeText(qrCodeDetected: Boolean, qrCodeContent: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = if (qrCodeDetected) "QR Code Detected: $qrCodeContent" else "No QR Code Detected",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun CameraPreview(
    barcodeScanner: BarcodeScanner,
    setQrCodeDetected: (Boolean) -> Unit,
    setQrCodeContent: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var cameraError by remember { mutableStateOf(false) }
    val cameraController = remember { LifecycleCameraController(context) }
    val previewView = remember { PreviewView(context) }
    cameraController.cameraSelector =
        CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build()

    //Throttle the analysis to avoid constant checks.
    val resolutionStrategy = ResolutionStrategy(
        Size(500, 500),
        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
    )
    val resolutionSelector =
        ResolutionSelector.Builder().setResolutionStrategy(resolutionStrategy).build()
    cameraController.setImageAnalysisResolutionSelector(resolutionSelector)

    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        MlKitAnalyzer(
            listOf(barcodeScanner),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            ContextCompat.getMainExecutor(context),
        ) { result: MlKitAnalyzer.Result? ->
            val barcodeResults = result?.getValue(barcodeScanner)
            if ((barcodeResults == null) ||
                (barcodeResults.isEmpty()) ||
                (barcodeResults.first() == null)
            ) {
                setQrCodeDetected(false)
                setQrCodeContent("") // Clear the text.
                previewView.overlay.clear()
                previewView.setOnTouchListener { _, _ -> false }
                return@MlKitAnalyzer
            }
            val qrCode = barcodeResults[0]
            val qrCodeViewModel = QrCodeViewModel(qrCode)
            val qrCodeDrawable = QrCodeDrawable(qrCodeViewModel)
            setQrCodeContent(qrCode.rawValue ?: "") // Display the content.
            setQrCodeDetected(true)
            previewView.setOnTouchListener(qrCodeViewModel.qrCodeTouchCallback)
            previewView.overlay.clear()
            previewView.overlay.add(qrCodeDrawable)

        },
    )

    cameraController.bindToLifecycle(lifecycleOwner).also {
        //Check if the camera was able to start or if there is a problem.
        try {
            cameraController.cameraInfo
        } catch (e: Exception) {
            Log.e("Test", "Camera error: $e")
            cameraError = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (cameraError) {
            Text(
                text = "Error: could not initialize camera",
                modifier = Modifier
                    .padding(16.dp),
            )
        } else {
            AndroidView(
                factory = {
                    previewView.apply {
                        this.controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}