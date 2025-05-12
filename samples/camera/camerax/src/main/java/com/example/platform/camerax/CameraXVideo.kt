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
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The main screen composable for the camera functionality.
 * Manages its own state including permissions (using Accompanist),
 * camera executor, preview, and recording controls.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXVideo() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- State Management within the Composable ---
    // UI state reflecting whether recording is active
    var recordingState by remember { mutableStateOf<RecordingState>(RecordingState.Idle) }
    // Holds the active VideoCapture use case instance
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    // Holds the active Recording session instance
    var recording by remember { mutableStateOf<Recording?>(null) }

    // --- Camera Executor ---
    // Remember a single-threaded executor for camera operations
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // --- Cleanup ---
    // Use DisposableEffect to shut down the executor when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Shutting down camera executor.")
            cameraExecutor.shutdown()
        }
    }

    // --- Permission Handling with Accompanist ---
    val permissionsState = rememberMultiplePermissionsState(
        permissions = REQUIRED_PERMISSIONS.toList(), // Use the existing list
    )

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize()) {
        // Check if all required permissions are granted
        if (permissionsState.allPermissionsGranted) {
            Log.d(TAG, "All permissions granted, showing camera preview.")
            // Display camera preview and controls
            CameraContent(
                lifecycleOwner = lifecycleOwner,
                cameraExecutor = cameraExecutor,
                recordingState = recordingState,
                onVideoCaptureCreated = { newVideoCapture ->
                    Log.d(TAG, "VideoCapture instance created.")
                    videoCapture = newVideoCapture
                },
                onRecordClick = {
                    val currentVideoCapture = videoCapture // Use internal state
                    if (currentVideoCapture != null) {
                        if (recordingState == RecordingState.Idle) {
                            Log.d(TAG, "Start Recording button clicked.")
                            // Start Recording Action
                            startRecording(
                                context = context,
                                videoCapture = currentVideoCapture,
                                executor = cameraExecutor,
                                onRecordingStarted = { activeRec ->
                                    // Update internal recording state
                                    recording = activeRec
                                    recordingState = RecordingState.Recording
                                },
                                onRecordingError = { errorEvent ->
                                    Log.e(TAG, "VideoCapture Error: ${errorEvent.cause}")
                                    // Reset internal state on error
                                    recording = null
                                    recordingState = RecordingState.Idle
                                },
                                onRecordingComplete = {
                                    Log.d(TAG, "Recording complete.")
                                    // Reset internal state on completion
                                    recording = null
                                    recordingState = RecordingState.Idle
                                },
                            )
                        } else {
                            Log.d(TAG, "Stop Recording button clicked.")
                            // Stop Recording Action
                            recording?.stop() // Use the internal recording instance to stop
                            // State is updated via the callbacks passed to startRecording
                        }
                    } else {
                        Log.e(TAG, "Record button clicked but VideoCapture is null.")
                        Toast.makeText(context, "Camera not ready.", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        } else {
            // Permissions are not granted, show rationale or request button
            PermissionRationale(permissionsState)
        }
    }
}

/**
 * Composable responsible for displaying the camera preview and controls
 * when permissions are granted.
 */
@Composable
private fun CameraContent(
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    recordingState: RecordingState,
    onVideoCaptureCreated: (VideoCapture<Recorder>) -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        CameraPreview(
            lifecycleOwner = lifecycleOwner,
            cameraExecutor = cameraExecutor,
            onVideoCaptureCreated = onVideoCaptureCreated,
        )

        // Control buttons overlay at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RecordButton(
                recordingState = recordingState,
                onRecordClick = onRecordClick,
            )
            // TODO: Add Quality Selector Composable if needed
            // TODO: Add Camera Flip Button Composable if needed
        }
    }
}

/**
 * Composable responsible for displaying the permission rationale or request button
 * when permissions are not granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRationale(
    permissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val textToShow = if (permissionsState.shouldShowRationale) {
            // If the user has denied the permission but the rationale should be shown,
            // explain why the permission is needed
            "Camera and Audio access are important for this app. Please grant the permissions."
        } else {
            // If it's the first time the user sees this or the user has denied the
            // permission permanently, explain why the permission is needed
            "Camera and Audio permissions required for this feature to be available. " +
                    "Please grant the permissions."
        }
        Text(
            textToShow,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = {
                Log.d(TAG, "Request Permissions button clicked.")
                permissionsState.launchMultiplePermissionRequest()
            },
        ) {
            Text("Request permissions")
        }
    }
}


/**
 * Composable function that hosts the CameraX PreviewView using AndroidView.
 * It also handles the binding of CameraX use cases (Preview, VideoCapture)
 * to the lifecycle owner.
 *
 * @param modifier Modifier for the AndroidView.
 * @param lifecycleOwner The LifecycleOwner to bind CameraX to.
 * @param cameraExecutor The executor for CameraX binding operations.
 * @param onVideoCaptureCreated Callback invoked when the VideoCapture use case is created,
 * passing the instance back up.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onVideoCaptureCreated: (VideoCapture<Recorder>) -> Unit, // Callback still needed to pass VC up
) {
    val context = LocalContext.current
    // Remember the PreviewView instance
    val previewView = remember { PreviewView(context) }
    Log.d(TAG, "CameraPreview Composable recomposing/launching.")

    // Use LaunchedEffect to bind the camera. Add lifecycleOwner and context as keys
    // so that binding restarts if they change (though context change is unlikely here).
    LaunchedEffect(lifecycleOwner, context) {
        Log.d(TAG, "LaunchedEffect for camera binding starting.")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    Log.d(TAG, "CameraProvider obtained.")

                    // --- Preview Use Case Setup ---
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                            Log.d(TAG, "Preview surface provider set.")
                        }

                    // --- VideoCapture Use Case Setup ---
                    val recorder = Recorder.Builder()
                        // Consider adding QualitySelector based on requirements
                        // .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)
                    // Pass the created VideoCapture instance up to the caller (CameraScreen)
                    Log.d(TAG, "VideoCapture created, invoking callback.")
                    onVideoCaptureCreated(videoCapture)

                    // --- Camera Selector ---
                    // TODO: Allow selecting between front and back cameras
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    Log.d(TAG, "Using default back camera.")

                    // Unbind any existing use cases before rebinding
                    Log.d(TAG, "Unbinding all previous use cases.")
                    cameraProvider.unbindAll()

                    // Bind the desired use cases (Preview and VideoCapture) to the lifecycle
                    Log.d(TAG, "Binding Preview and VideoCapture use cases.")
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, videoCapture,
                    )
                    Log.d(TAG, "CameraX Use cases bound successfully.")

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                    // Handle binding failure (e.g., show error message to user)
                    Toast.makeText(
                        context,
                        "Failed to initialize camera: ${exc.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }

            },
            ContextCompat.getMainExecutor(context),
        ) // Use main executor for CameraX listener setup
        Log.d(TAG, "CameraProvider listener added.")
    }

    // Embed the Android PreviewView within the Compose UI hierarchy
    AndroidView(
        {
            Log.d(TAG, "AndroidView factory executing.")
            previewView
        },
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * Composable for the recording button. Changes appearance based on recording state.
 *
 * @param recordingState The current state of recording (Idle or Recording).
 * @param onRecordClick Lambda function to be executed when the button is clicked.
 */
@Composable
fun RecordButton(
    recordingState: RecordingState,
    onRecordClick: () -> Unit,
) {
    Button(
        onClick = onRecordClick,
        colors = ButtonDefaults.buttonColors(
            // Change color based on recording state
            containerColor = if (recordingState == RecordingState.Recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(if (recordingState == RecordingState.Recording) "Stop Recording" else "Start Recording")
    }
}

// --- Helper Functions & Constants ---

/**
 * Enum to represent the current recording state for UI purposes.
 */
enum class RecordingState {
    Idle, Recording
}

/**
 * Starts a new video recording using CameraX VideoCapture.
 *
 * @param context The application context.
 * @param videoCapture The VideoCapture use case instance.
 * @param executor The executor to run recording operations on.
 * @param onRecordingStarted Callback invoked when recording starts, providing the Recording object.
 * @param onRecordingError Callback invoked if recording fails, providing the error event.
 * @param onRecordingComplete Callback invoked when recording finishes successfully.
 */
private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    executor: ExecutorService,
    onRecordingStarted: (Recording) -> Unit,
    onRecordingError: (VideoRecordEvent.Finalize) -> Unit,
    onRecordingComplete: () -> Unit,
) {
    // Create MediaStore output options for saving the video
    val mediaStoreOutputOptions = createMediaStoreOutputOptions(context)
    Log.d(TAG, "Preparing recording to: $mediaStoreOutputOptions")

    // Prepare the recording
    val pendingRecording = videoCapture.output
        .prepareRecording(context, mediaStoreOutputOptions)
        .apply {
            // Enable audio recording if permission is granted
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PermissionChecker.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Audio permission granted, enabling audio.")
                withAudioEnabled()
            } else {
                Log.d(TAG, "Audio permission denied, recording without audio.")
            }
        }

    // Start the recording and get the Recording object
    val activeRecording = pendingRecording.start(executor) { recordEvent ->
        // Handle recording events
        when (recordEvent) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "Recording started successfully.")
            }

            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    // Recording succeeded
                    val msg = "Video capture succeeded: ${recordEvent.outputResults.outputUri}"
                    Log.d(TAG, msg)
                    // Dispatch Toast to the main thread
                    ContextCompat.getMainExecutor(context).execute {
                        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
                    }
                    onRecordingComplete()
                } else {
                    // Recording failed
                    val errorCause = recordEvent.cause ?: "Unknown error"
                    val errorCode = recordEvent.error
                    Log.e(
                        TAG,
                        "Video capture error ($errorCode): $errorCause",
                        recordEvent.cause,
                    )
                    // Dispatch Toast to the main thread
                    ContextCompat.getMainExecutor(context).execute {
                        Toast.makeText(
                            context.applicationContext,
                            "Recording failed: $errorCause",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    onRecordingError(recordEvent)
                }
            }

            is VideoRecordEvent.Status -> {
                // Provides recording progress, duration, file size, etc.
            }

            is VideoRecordEvent.Pause -> Log.d(TAG, "Recording paused")
            is VideoRecordEvent.Resume -> Log.d(TAG, "Recording resumed")
        }
    }

    // Pass the obtained Recording object to the callback immediately after starting
    onRecordingStarted(activeRecording)
    Log.d(TAG, "Recording initiated.")
}

/**
 * Creates MediaStoreOutputOptions for saving the recorded video.
 * Defines the filename, MIME type, and storage location.
 *
 * @param context The application context.
 * @return Configured MediaStoreOutputOptions.
 */
private fun createMediaStoreOutputOptions(context: Context): MediaStoreOutputOptions {
    val name = "CameraX-recording-" +
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        // Define storage location (Movies/CameraX-Video directory) for Android Q+
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }
    }
    Log.d(TAG, "Creating MediaStoreOutputOptions with name: $name")

    return MediaStoreOutputOptions
        .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        .setContentValues(contentValues)
        .build()
}

// Removed: checkPermissions function is no longer needed with Accompanist


// --- Constants ---
private const val TAG = "CameraXComposeAccompanist" // Updated TAG for clarity
private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

// Required permissions for camera and video recording
val REQUIRED_PERMISSIONS =
    mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    ).apply {
        // Add storage permission for older Android versions if necessary
        // (Scoped storage is preferred on newer versions)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            Log.d(TAG, "Adding WRITE_EXTERNAL_STORAGE permission for API <= P.")
        }
    }.toTypedArray()

