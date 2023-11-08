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

package com.example.platform.graphics.ultrahdr.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.DataSpace
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.opengl.FrameBuffer
import androidx.graphics.opengl.GLFrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.SyncFenceCompat
import com.example.platform.graphics.ultrahdr.databinding.UltrahdrWithGraphicsBinding
import com.google.android.catalog.framework.annotations.Sample
import java.util.function.Consumer

@Sample(
    name = "UltraHDR with Graphics",
    description = "This sample demonstrates displaying an UltraHDR image via and OpenGL Pipeline",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class UltraHDRWithOpenGL : Fragment(),
    GLRenderer.EGLContextCallback,
    GLFrameBufferRenderer.Callback,
    SurfaceHolder.Callback {
    /**
     *  Android ViewBinding.
     */
    private var _binding: UltrahdrWithGraphicsBinding? = null
    private val binding get() = _binding!!

    private lateinit var glRenderer: GLRenderer
    private lateinit var glFrameBufferRenderer: GLFrameBufferRenderer
    private lateinit var ultraHDRGLRenderer: UltraHDRWithOpenGLRenderer
    private var hdrSdrRatio = 1.0f

    private lateinit var bitmap: Bitmap

    private val updateHdrSdrRatio = Consumer<Display> { glFrameBufferRenderer.render() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UltrahdrWithGraphicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.colorModeControls.setWindow(requireActivity().window)

        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_LAMPS)
        bitmap = BitmapFactory.decodeStream(stream)

        binding.surfaceView.holder.addCallback(this)
    }

    private fun initUltraHDRGlRenderer() {
        ultraHDRGLRenderer = UltraHDRWithOpenGLRenderer(requireContext(), bitmap)
        binding.imageContainer.setImageBitmap(bitmap)
    }

    private fun initGlRenderer() {
        glRenderer = GLRenderer().apply {
            registerEGLContextCallback(this@UltraHDRWithOpenGL)
            start()
        }
    }

    private fun initGlFrameRenderer() {
        glFrameBufferRenderer = GLFrameBufferRenderer.Builder(binding.surfaceView, this)
            .setGLRenderer(glRenderer)
            .build()
    }

    override fun onAttach(context: Context) {
        requireActivity().display?.registerHdrSdrRatioChangedListener(
            Runnable::run,
            updateHdrSdrRatio,
        )

        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
        requireActivity().display?.unregisterHdrSdrRatioChangedListener(updateHdrSdrRatio)
    }

    override fun onDestroyView() {
        glFrameBufferRenderer.release(true)
        super.onDestroyView()
    }

    override fun onEGLContextCreated(eglManager: EGLManager) {
        ultraHDRGLRenderer.onContextCreated()
    }

    override fun onEGLContextDestroyed(eglManager: EGLManager) {
        ultraHDRGLRenderer.onContextDestroyed()
    }

    override fun onDrawFrame(
        eglManager: EGLManager,
        width: Int,
        height: Int,
        bufferInfo: BufferInfo,
        transform: FloatArray,
    ) {
        hdrSdrRatio = requireActivity().display?.hdrSdrRatio ?: 1.0f
        ultraHDRGLRenderer.onDrawFrame(
            width,
            height,
            hdrSdrRatio,
            bufferInfo.width,
            bufferInfo.height,
            transform,
        )
    }

    override fun onDrawComplete(
        targetSurfaceControl: SurfaceControlCompat,
        transaction: SurfaceControlCompat.Transaction,
        frameBuffer: FrameBuffer,
        syncFence: SyncFenceCompat?,
    ) {
        transaction.setDataSpace(targetSurfaceControl, SRGB_ERB)
        transaction.setExtendedRangeBrightness(
            targetSurfaceControl,
            hdrSdrRatio,
            ultraHDRGLRenderer.desiredHdrSdrRatio,
        )

        super.onDrawComplete(targetSurfaceControl, transaction, frameBuffer, syncFence)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Initialize GL pipeline when surface is created.
        initUltraHDRGlRenderer()
        initGlRenderer()
        initGlFrameRenderer()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        binding.surfaceView.setAspectRatio(bitmap.width, bitmap.height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_LAMPS = "gainmaps/train_station_night.jpg"

        /**
         * DataSpace of SRGB_ERB.
         */
        private val SRGB_ERB = DataSpace.pack(
            DataSpace.STANDARD_BT2020_CONSTANT_LUMINANCE,
            DataSpace.TRANSFER_LINEAR,
            DataSpace.RANGE_EXTENDED,
        )
    }
}