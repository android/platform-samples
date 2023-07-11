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
package com.example.platform.camera.preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.camera.common.DirectExecutor
import com.example.platform.camera.common.OrientationLiveData
import com.example.platform.camera.common.SIZE_720P
import com.example.platform.camera.common.getPreviewOutputSize
import com.example.platform.camera.databinding.Camera2PreviewBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Sample(
    name = "Camera Preview",
    description = "Demonstrates displaying processed pixel data directly from the camera sensor "
            + "to the screen using Camera2.",
    documentation = "https://developer.android.com/training/camera2",
    tags = ["Camera2"],
)
class Camera2Preview : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: Camera2PreviewBinding? = null
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

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::handleCameraPermissionResult,
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = Camera2PreviewBinding.inflate(inflater, container, false)
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
        binding.fragmentCamera2PreviewAction.visibility = View.VISIBLE
        binding.fragmentCamera2PreviewAction.text = message
        binding.fragmentCamera2PreviewAction.setOnClickListener {
            action()
        }
    }

    private fun setUpCamera() {
        binding.fragmentCamera2PreviewViewfinder.holder.addCallback(
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
                        binding.fragmentCamera2PreviewViewfinder.display,
                        characteristics,
                        SurfaceHolder::class.java,
                    )

                    Log.d(
                        TAG,
                        "Preview size: ${binding.fragmentCamera2PreviewViewfinder.width} x " +
                                "${binding.fragmentCamera2PreviewViewfinder.height}",
                    )

                    Log.d(TAG, "Selected Preview Size: $previewSize")
                    binding.fragmentCamera2PreviewViewfinder.setAspectRatio(
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

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating capture request
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        binding.fragmentCamera2PreviewAction.visibility = View.GONE

        try {
            // Open the selected camera
            camera = openCamera(cameraManager, cameraIds.first(), cameraHandler)

            // Creates list of Surfaces where the camera will output frames
            val targets = listOf(binding.fragmentCamera2PreviewViewfinder.holder.surface)

            // Start a capture session using our open camera and list of Surfaces where frames will go
            session = createCaptureSession(camera, targets, cameraHandler)
            val captureRequest = camera.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW,
            ).apply { addTarget(binding.fragmentCamera2PreviewViewfinder.holder.surface) }

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
    @RequiresPermission(Manifest.permission.CAMERA)
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null,
    ): CameraDevice = suspendCancellableCoroutine { cont ->
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
        private val TAG = Camera2Preview::class.java.simpleName
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
