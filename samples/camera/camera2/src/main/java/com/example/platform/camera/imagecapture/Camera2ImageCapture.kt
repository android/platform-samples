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
package com.example.platform.camera.imagecapture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.camera.common.*
import com.example.platform.camera.databinding.Camera2ImageCaptureBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Sample(
    name = "Image Capture",
    description = "This sample demonstrates how to capture an image using Camera2 and encode it " +
            "into a JPEG container.",
    documentation = "https://developer.android.com/training/camera2/capture-sessions-requests",
    tags = ["Camera2"]
)
class Camera2ImageCapture : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: Camera2ImageCaptureBinding? = null
    private val binding get() = _binding!!

    /**
     *  Detects, characterizes, and connects to a CameraDevice (used for all camera operations).
     */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * List of camera id provided by the [CameraManager].
     */
    private val cameraIds: List<String> by lazy {
        cameraManager.cameraIdList.asList()
    }

    /**
     * [CameraCharacteristics] corresponding to the provided Camera ID.
     */
    private val characteristics: CameraCharacteristics by lazy {
        // This will use the first camera id from cameraIds.
        cameraManager.getCameraCharacteristics(cameraIds.first())
    }

    /**
     * [HandlerThread] where all camera operations run
     */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /**
     * [Handler] corresponding to [cameraThread]
     */
    private val cameraHandler = Handler(cameraThread.looper)

    /**
     * [HandlerThread] where all buffer reading operations run
     */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /**
     * [Handler] corresponding to [imageReaderThread]
     */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /**
     * The [CameraDevice] that will be opened in this fragment.
     */
    private lateinit var camera: CameraDevice

    /**
     * Internal reference to the ongoing [CameraCaptureSession] configured with our parameters.
     */
    private lateinit var session: CameraCaptureSession

    /**
     * [OrientationLiveData] listener for changes in the device orientation relative to the camera.
     */
    private lateinit var relativeOrientation: OrientationLiveData

    /**
     * Readers used as buffers for camera still shots
     */
    private lateinit var imageReader: ImageReader

    /**
     * Performs A flash animation to mimic a common shutter when taking a photo.
     */
    private val animationTask: Runnable by lazy {
        Runnable {
            // Flash white animation
            binding.fragmentCamera2ImageCaptureOverlay.background =
                Color.argb(150, 255, 255, 255).toDrawable()

            // Wait for ANIMATION_FAST_MILLIS
            binding.fragmentCamera2ImageCaptureOverlay.postDelayed(
                {
                    // Remove white flash animation
                    binding.fragmentCamera2ImageCaptureOverlay.background = null
                },
                ANIMATION_FAST_MILLIS,
            )
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::handleCameraPermissionResult,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = Camera2ImageCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpCamera()
    }

    override fun onStart() {
        super.onStart()
        when {
            isPermissionGranted() -> view?.post(::initializeCamera)
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showActionMessage("This sample requires CAMERA permission to work. Please grant it") {
                    requestCameraPermission()
                }
            }

            else -> {
                showActionMessage("Grant permission") {
                    requestCameraPermission()
                }
            }
        }
    }

    /**
     *  Attempt to request camera permissions.
     */
    private fun requestCameraPermission() =
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

    /**
     *  Handle result of camera permissions request.
     */
    private fun handleCameraPermissionResult(isGranted: Boolean) {
        when (isGranted) {
            true -> view?.post(::initializeCamera)
            false -> view?.let {
                showActionMessage("Permissions not granted: Try again") {
                    requestCameraPermission()
                }
            }
        }
    }

    private fun showActionMessage(message: String, action: () -> Unit) {
        binding.fragmentCamera2ImageCapturePermissionButton.visibility = View.VISIBLE
        binding.fragmentCamera2ImageCapturePermissionButton.text = message
        binding.fragmentCamera2ImageCapturePermissionButton.setOnClickListener {
            action()
        }
    }

    private fun showImage(imageLocation: String) {
        binding.fragmentCamera2ImageCaptureButton.visibility = View.GONE
        binding.fragmentCamera2PostCaptureBackButton.visibility = View.VISIBLE
        binding.fragmentCamera2ImageCaptureImageView.setContent {
            SimpleImageViewer(location = imageLocation)
        }
    }

    private fun returnToImageCapture() {
        binding.fragmentCamera2ImageCaptureButton.visibility = View.VISIBLE
        binding.fragmentCamera2PostCaptureBackButton.visibility = View.GONE
        binding.fragmentCamera2ImageCaptureImageView.setContent {  }
    }

    private fun setUpCamera() {
        binding.fragmentCamera2ImageCaptureButton.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            insets.consumeSystemWindowInsets()
        }

        binding.fragmentCamera2PostCaptureBackButton.setOnClickListener {
            returnToImageCapture()
        }

        binding.fragmentCamera2ImageCaptureViewfinder.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) = Unit

                override fun surfaceCreated(holder: SurfaceHolder) {
                    // Selects appropriate preview size and configures view finder
                    val previewSize = getPreviewOutputSize(
                        binding.fragmentCamera2ImageCaptureViewfinder.display,
                        characteristics,
                        SurfaceHolder::class.java,
                    )

                    Log.d(
                        TAG,
                        "Preview size: ${binding.fragmentCamera2ImageCaptureViewfinder.width} " +
                                "x " + "${binding.fragmentCamera2ImageCaptureViewfinder.height}",
                    )

                    Log.d(TAG, "Selected Preview Size: $previewSize")
                    binding.fragmentCamera2ImageCaptureViewfinder.setAspectRatio(
                        previewSize?.width ?: SIZE_720P.size.width,
                        previewSize?.height ?: SIZE_720P.size.height,
                    )
                }
            },
        )

        // Used to rotate the output media to match device orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner) { orientation ->
                Log.d(TAG, "Orientation changed: $orientation")
            }
        }
    }

    private fun setUpImageReader() {
        // Initialize an image reader which will be used to capture still photos
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        configMap?.let { config ->
            config.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }
                ?.let { size ->
                    imageReader = ImageReader.newInstance(
                        size.width, size.height, ImageFormat.JPEG, IMAGE_BUFFER_SIZE,
                    )
                }
        }

        // Listen to the capture button
        binding.fragmentCamera2ImageCaptureButton.setOnClickListener {
            // Disable click listener to prevent multiple requests simultaneously in flight
            it.isEnabled = false

            // Perform I/O heavy operations in a different scope
            lifecycleScope.launch(Dispatchers.IO) {
                takePhoto().use { result ->
                    Log.d(TAG, "Result received: $result")

                    // Save the result to disk
                    val output = saveResult(result)
                    Log.d(TAG, "Image saved: ${output.absolutePath}")

                    // If the result is a JPEG file, update EXIF metadata with orientation info
                    if (output.extension == "jpg") {
                        val exif = ExifInterface(output.absolutePath)
                        exif.setAttribute(
                            ExifInterface.TAG_ORIENTATION, result.orientation.toString(),
                        )
                        exif.saveAttributes()
                        Log.d(TAG, "EXIF metadata saved: ${output.absolutePath}")
                    }

                    // Display the photo taken to user
                    lifecycleScope.launch(Dispatchers.Main) {
                        showImage(output.absolutePath)
                    }
                }

                // Re-enable click listener after photo is taken
                it.post { it.isEnabled = true }
            }
        }
    }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating capture request
     */
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        binding.fragmentCamera2ImageCapturePermissionButton.visibility = View.GONE
        try {
            // Open the selected camera
            camera = openCamera(cameraManager, cameraIds.first(), cameraHandler)

            // Set up the image reader
            setUpImageReader()

            // Creates list of Surfaces where the camera will output frames
            val targets = listOf(
                binding.fragmentCamera2ImageCaptureViewfinder.holder.surface,
                imageReader.surface,
            )

            // Start a capture session using our open camera and list of Surfaces where frames will
            // go.
            session = createCaptureSession(camera, targets, cameraHandler)

            val captureRequest = camera.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW,
            ).apply { addTarget(binding.fragmentCamera2ImageCaptureViewfinder.holder.surface) }

            // This will keep sending the capture request as frequently as possible until the
            // session is torn down or session.stopRepeating() is called
            session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "initializeCamera failed", e)
            showActionMessage("Camera init failed: Try again") {
                view?.post(::initializeCamera)
            }
        }
    }

    /**
     * Opens the camera and returns the opened device (as the result of the suspend coroutine)
     */
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null,
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        try {
            manager.openCamera(
                cameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) = cont.resume(device)

                    override fun onDisconnected(device: CameraDevice) {
                        Log.w(TAG, "Camera $cameraId has been disconnected")
                    }

                    override fun onError(device: CameraDevice, error: Int) {
                        val msg = when (error) {
                            ERROR_CAMERA_DEVICE -> "Fatal (device)"
                            ERROR_CAMERA_DISABLED -> "Device policy"
                            ERROR_CAMERA_IN_USE -> "Camera in use"
                            ERROR_CAMERA_SERVICE -> "Fatal (service)"
                            ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                            else -> "Unknown"
                        }
                        val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                        Log.e(TAG, exc.message, exc)
                        if (cont.isActive) cont.resumeWithException(exc)
                    }
                },
                handler,
            )
        } catch (e: SecurityException) {
            requestCameraPermission()
        }
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null,
    ): CameraCaptureSession = suspendCoroutine { cont ->
        // First, create a CameraCaptureSession.StateCallback object for the onConfigured &
        // onConfigureFailed callbacks.
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val configs = targets.map {
                val config = OutputConfiguration(it)
                config
            }

            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                configs,
                DirectExecutor(),
                callback,
            )

            device.createCaptureSession(sessionConfiguration)
        } else {
            device.createCaptureSession(
                targets,
                callback,
                handler,
            )
        }
    }

    /**
     * Helper function used to capture a still image using the [CameraDevice.TEMPLATE_STILL_CAPTURE]
     * template. It performs synchronization between the [CaptureResult] and the [Image] resulting
     * from the single capture, and outputs a [CombinedCaptureResult] object.
     */
    private suspend fun takePhoto(): CombinedCaptureResult = suspendCoroutine { cont ->
        // Start a new image queue.
        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener(
            {
                val image = it.acquireLatestImage()
                Log.d(TAG, "Image available in queue: ${image.timestamp}")
                imageQueue.add(image)
            },
            imageReaderHandler,
        )

        val request = session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            .apply { addTarget(imageReader.surface) }

        session.capture(
            request.build(),
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    timestamp: Long,
                    frameNumber: Long,
                ) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber)
                    binding.fragmentCamera2ImageCaptureViewfinder.post(animationTask)
                }

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    super.onCaptureCompleted(session, request, result)
                    val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                    Log.d(TAG, "Capture result received: $resultTimestamp")

                    // Set a timeout in case image captured is dropped from the pipeline
                    val exc = TimeoutException("Image de-queuing took too long")
                    val timeoutRunnable = Runnable { cont.resumeWithException(exc) }
                    imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                    // Loop in the coroutine's context until an image with matching timestamp comes
                    // We need to launch the coroutine context again because the callback is done in
                    // the handler provided to the `capture` method, not in our coroutine context.
                    @Suppress("BlockingMethodInNonBlockingContext")
                    lifecycleScope.launch(cont.context) {
                        while (true) {
                            // Dequeue images while timestamps don't match
                            val image = imageQueue.take()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                image.format != ImageFormat.DEPTH_JPEG &&
                                image.timestamp != resultTimestamp
                            ) continue

                            Log.d(TAG, "Matching image dequeued: ${image.timestamp}")

                            // Unset the image reader listener
                            imageReaderHandler.removeCallbacks(timeoutRunnable)
                            imageReader.setOnImageAvailableListener(null, null)

                            // Clear the queue of images, if there are left.
                            while (imageQueue.size > 0) {
                                imageQueue.take().close()
                            }

                            // Compute EXIF orientation metadata.
                            val rotation = relativeOrientation.value ?: 0
                            val mirrored = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                                    CameraCharacteristics.LENS_FACING_FRONT
                            val exifOrientation = computeExifOrientation(rotation, mirrored)

                            // Build the result and resume progress
                            cont.resume(
                                CombinedCaptureResult(
                                    image,
                                    result,
                                    exifOrientation,
                                    imageReader.imageFormat,
                                ),
                            )

                            // There is no need to break out of the loop because this coroutine will
                            // suspend.
                        }
                    }
                }
            },
            cameraHandler,
        )
    }

    /**
     * Helper function used to save a [CombinedCaptureResult] into a [File]
     */
    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        when (result.format) {

            // When the format is JPEG or DEPTH JPEG we can simply save the bytes as-is
            ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                try {
                    val output = createFile(requireContext(), "jpg")
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write JPEG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // When the format is RAW we use the DngCreator utility library
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(characteristics, result.metadata)
                try {
                    val output = createFile(requireContext(), "dng")
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write DNG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }

            // No other formats are supported by this sample
            else -> {
                val exc = RuntimeException("Unknown image format: ${result.image.format}")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraThread.quitSafely()
    }

    private fun isPermissionGranted() = ContextCompat.checkSelfPermission(
        requireContext(), CAMERA_PERMISSION,
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private val TAG = Camera2ImageCapture::class.java.simpleName
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

        /**
         * Milliseconds used for UI animations
         */
        const val ANIMATION_FAST_MILLIS = 50L

        /**
         *  Maximum number of images that will be held in the reader's buffer
         */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        /**
         * Maximum time allowed to wait for the result of an image capture
         */
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        /**
         * Create a [File] named a using formatted timestamp with the current date and time.
         *
         * @return [File] created.
         */
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(context.filesDir, "IMG_${sdf.format(Date())}.$extension")
        }
    }
}
