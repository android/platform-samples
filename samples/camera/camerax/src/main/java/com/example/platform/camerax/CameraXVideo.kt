package com.example.platform.camerax

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Corrected import
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

private enum class CameraScreenMode {
    RECORDING,
    PLAYBACK
}

/**
 * The main screen composable for the camera functionality.
 * Manages its own state including permissions (using Accompanist),
 * camera executor, preview, recording controls, and screen navigation.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXVideo() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- State Management ---
    var recordingState by remember { mutableStateOf<RecordingState>(RecordingState.Idle) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }

    // --- Screen navigation and video URI state ---
    var currentScreen by remember { mutableStateOf(CameraScreenMode.RECORDING) }
    var lastRecordedVideoUri by remember { mutableStateOf<Uri?>(null) }


    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Shutting down camera executor.")
            cameraExecutor.shutdown()
        }
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = REQUIRED_PERMISSIONS.toList(),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            CameraScreenMode.RECORDING -> {
                if (permissionsState.allPermissionsGranted) {
                    Log.d(TAG, "All permissions granted, showing camera preview.")
                    CameraContent(
                        lifecycleOwner = lifecycleOwner,
                        // cameraExecutor removed as it's not directly used by CameraContent
                        recordingState = recordingState,
                        onVideoCaptureCreated = { newVideoCapture ->
                            Log.d(TAG, "VideoCapture instance created.")
                            videoCapture = newVideoCapture
                        },
                        onRecordClick = {
                            val currentVideoCapture = videoCapture
                            if (currentVideoCapture != null) {
                                if (recordingState == RecordingState.Idle) {
                                    Log.d(TAG, "Start Recording button clicked.")
                                    startRecording(
                                        context = context,
                                        videoCapture = currentVideoCapture,
                                        executor = cameraExecutor, // cameraExecutor passed here
                                        onRecordingStarted = { activeRec ->
                                            recording = activeRec
                                            recordingState = RecordingState.Recording
                                        },
                                        onRecordingError = { errorEvent ->
                                            Log.e(TAG, "VideoCapture Error: ${errorEvent.cause}")
                                            recording = null
                                            recordingState = RecordingState.Idle
                                        },
                                        onRecordingComplete = { uri ->
                                            Log.d(TAG, "Recording complete. URI: $uri")
                                            lastRecordedVideoUri = uri
                                            currentScreen =
                                                CameraScreenMode.PLAYBACK
                                            recording = null
                                            recordingState = RecordingState.Idle
                                        },
                                    )
                                } else {
                                    Log.d(TAG, "Stop Recording button clicked.")
                                    recording?.stop()
                                }
                            } else {
                                Log.e(TAG, "Record button clicked but VideoCapture is null.")
                                Toast.makeText(context, "Camera not ready.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                    )
                } else {
                    PermissionRationale(permissionsState)
                }
            }

            CameraScreenMode.PLAYBACK -> {
                lastRecordedVideoUri?.let { uri ->
                    VideoPlaybackScreen(
                        // Loop enabled here
                        videoUri = uri,
                        onBackToRecord = {
                            currentScreen = CameraScreenMode.RECORDING
                            lastRecordedVideoUri = null
                        },
                    )
                } ?: run {
                    Log.e(TAG, "VideoPlaybackScreen requested but videoUri is null.")
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Error: Video URI not available. Please record again.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                currentScreen = CameraScreenMode.RECORDING
                            },
                        ) {
                            Text("Go Back to Record")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    lifecycleOwner: LifecycleOwner,
    // cameraExecutor: ExecutorService, // Not needed here if startRecording is called from parent
    recordingState: RecordingState,
    onVideoCaptureCreated: (VideoCapture<Recorder>) -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CameraPreview(
            // cameraExecutor is needed by CameraPreview for binding
            lifecycleOwner = lifecycleOwner,
            // cameraExecutor passed here if CameraPreview handles binding independently
            // If binding is managed by CameraXVideo, this might not be needed directly
            onVideoCaptureCreated = onVideoCaptureCreated,
        )
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
        }
    }
}

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
            "Camera and Audio access are important for this app. Please grant the permissions."
        } else {
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


@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    onVideoCaptureCreated: (VideoCapture<Recorder>) -> Unit,
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }

    // If CameraX binding needs a specific executor, it should be sourced or passed here.
    // Using ContextCompat.getMainExecutor(context) is common for listeners.
    // The original 'cameraExecutor' from CameraXVideo could be passed if needed for binding.
    val localCameraExecutor =
        remember { Executors.newSingleThreadExecutor() } // Or pass from parent

    Log.d(TAG, "CameraPreview Composable recomposing/launching.")

    LaunchedEffect(lifecycleOwner, context) {
        Log.d(TAG, "LaunchedEffect for camera binding starting.")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    Log.d(TAG, "CameraProvider obtained.")

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                            Log.d(TAG, "Preview surface provider set.")
                        }

                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.HIGHEST,
                                FallbackStrategy.higherQualityOrLowerThan(Quality.SD),
                            ),
                        )
                        .build()
                    val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)
                    Log.d(TAG, "VideoCapture created, invoking callback.")
                    onVideoCaptureCreated(videoCapture)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    Log.d(TAG, "Using default back camera.")

                    Log.d(TAG, "Unbinding all previous use cases.")
                    cameraProvider.unbindAll()

                    Log.d(TAG, "Binding Preview and VideoCapture use cases.")
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, videoCapture,
                    )
                    Log.d(TAG, "CameraX Use cases bound successfully.")

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                    Toast.makeText(
                        context,
                        "Failed to initialize camera: ${exc.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }

            },
            ContextCompat.getMainExecutor(context),
        )
        Log.d(TAG, "CameraProvider listener added.")
    }

    DisposableEffect(Unit) {
        onDispose {
            localCameraExecutor.isShutdown.not()
        }
    }


    AndroidView(
        {
            Log.d(TAG, "AndroidView factory executing.")
            previewView
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun RecordButton(
    recordingState: RecordingState,
    onRecordClick: () -> Unit,
) {
    Button(
        onClick = onRecordClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (recordingState == RecordingState.Recording)
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(if (recordingState == RecordingState.Recording) "Stop Recording" else "Start Recording")
    }
}

@Composable
fun VideoPlaybackScreen(
    videoUri: Uri,
    onBackToRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoView = remember { VideoView(context) }

    DisposableEffect(videoUri) {
        val mediaController = MediaController(context)
        mediaController.setAnchorView(videoView)

        videoView.setVideoURI(videoUri)
        videoView.setMediaController(mediaController)
        videoView.requestFocus()

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true // Enable looping
            Log.d(TAG, "VideoView prepared, looping enabled.")
        }
        videoView.start()
        Log.d(TAG, "VideoView playback started with URI: $videoUri")
        onDispose {
            Log.d(TAG, "Disposing VideoView, stopping playback.")
            videoView.stopPlayback()
            videoView.setOnCompletionListener(null) // Clean up listener
            videoView.setOnPreparedListener(null)   // Clean up listener
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { videoView },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onBackToRecord,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Updated icon
                contentDescription = "Record New Video",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}


enum class RecordingState {
    Idle, Recording
}

private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    executor: ExecutorService,
    onRecordingStarted: (Recording) -> Unit,
    onRecordingError: (VideoRecordEvent.Finalize) -> Unit,
    onRecordingComplete: (Uri) -> Unit,
) {
    val mediaStoreOutputOptions = createMediaStoreOutputOptions(context)
    Log.d(
        TAG,
        "Preparing recording to: ${mediaStoreOutputOptions.contentValues.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)}",
    )

    val pendingRecording = videoCapture.output
        .prepareRecording(context, mediaStoreOutputOptions)
        .apply {
            if (PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PermissionChecker.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Audio permission granted, enabling audio.")
                withAudioEnabled()
            } else {
                Log.d(TAG, "Audio permission denied, recording without audio.")
            }
        }

    val activeRecording =
        pendingRecording.start(executor) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    Log.d(TAG, "Recording started successfully.")
                }

                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val outputUri = recordEvent.outputResults.outputUri
                        val msg = "Video capture succeeded: $outputUri"
                        Log.d(TAG, msg)
                        ContextCompat.getMainExecutor(context).execute {
                            Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT)
                                .show()
                        }
                        onRecordingComplete(outputUri)
                    } else {
                        val errorCause = recordEvent.cause ?: "Unknown error"
                        val errorCode = recordEvent.error
                        Log.e(
                            TAG,
                            "Video capture error ($errorCode): $errorCause",
                            recordEvent.cause,
                        )
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
                    Log.v(TAG, "Status: ${recordEvent.recordingStats}")
                }

                is VideoRecordEvent.Pause -> Log.d(TAG, "Recording paused")
                is VideoRecordEvent.Resume -> Log.d(TAG, "Recording resumed")
            }
        }
    onRecordingStarted(activeRecording)
    Log.d(TAG, "Recording initiated.")
}

private fun createMediaStoreOutputOptions(context: Context): MediaStoreOutputOptions {
    val name = "CameraX-recording-" +
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".mp4"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
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

private const val TAG = "CameraXComposeExtended"
private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

val REQUIRED_PERMISSIONS =
    mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            Log.d(TAG, "Adding WRITE_EXTERNAL_STORAGE permission for API <= P.")
        }
    }.toTypedArray()