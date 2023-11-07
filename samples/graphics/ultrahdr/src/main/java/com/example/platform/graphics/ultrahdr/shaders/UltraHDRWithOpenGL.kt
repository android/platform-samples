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

package com.example.platform.graphics.ultrahdr.shaders

import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.hardware.DataSpace
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.LayoutInflater
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
class UltraHDRWithOpenGL : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: UltrahdrWithGraphicsBinding? = null
    private val binding get() = _binding!!

    private var renderer: GLFrameBufferRenderer? = null

    private val SRGB_ERB = DataSpace.pack(
        DataSpace.STANDARD_BT2020_CONSTANT_LUMINANCE,
        DataSpace.TRANSFER_LINEAR,
        DataSpace.RANGE_EXTENDED,
    )

    private val updateHdrSdrRatio = Consumer<Display> {
        renderer?.render()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UltrahdrWithGraphicsBinding.inflate(inflater, container, false)

        requireActivity().window.colorMode = ActivityInfo.COLOR_MODE_HDR

        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_LAMPS)
        val bitmap = BitmapFactory.decodeStream(stream)

        val gainmapRenderer = UltraHDRWithOpenGLRenderer(bitmap)
        val glRenderer = GLRenderer().apply {
            registerEGLContextCallback(
                object : GLRenderer.EGLContextCallback {
                    override fun onEGLContextCreated(eglManager: EGLManager) {
                        gainmapRenderer.onContextCreated()
                    }

                    override fun onEGLContextDestroyed(eglManager: EGLManager) {
                        gainmapRenderer.onContextDestroyed()
                    }
                },
            )
            start()
        }

        renderer = GLFrameBufferRenderer.Builder(
            binding.surfaceView,
            object : GLFrameBufferRenderer.Callback {
                private var hdrSdrRatio = 1.0f

                override fun onDrawComplete(
                    targetSurfaceControl: SurfaceControlCompat,
                    transaction: SurfaceControlCompat.Transaction,
                    frameBuffer: FrameBuffer,
                    syncFence: SyncFenceCompat?,
                ) {
                    transaction.setDataSpace(targetSurfaceControl, SRGB_ERB)
                    transaction.setExtendedRangeBrightness(
                        targetSurfaceControl,
                        hdrSdrRatio, gainmapRenderer.desiredHdrSdrRatio,
                    )
                    super.onDrawComplete(targetSurfaceControl, transaction, frameBuffer, syncFence)
                }

                override fun onDrawFrame(
                    eglManager: EGLManager,
                    width: Int,
                    height: Int,
                    bufferInfo: BufferInfo,
                    transform: FloatArray,
                ) {
                    hdrSdrRatio = requireActivity().display?.hdrSdrRatio ?: 1.0f
                    val frame = binding.surfaceView.holder.surfaceFrame
                    gainmapRenderer.onDrawFrame(
                        frame.width(), frame.height(), hdrSdrRatio,
                        bufferInfo.width, bufferInfo.height, transform,
                    )
                }
            },
        )
            .setGLRenderer(glRenderer)
            .build()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        requireActivity().display!!.registerHdrSdrRatioChangedListener(
            Runnable::run,
            updateHdrSdrRatio,
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().display?.unregisterHdrSdrRatioChangedListener(updateHdrSdrRatio)
        requireActivity().window.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
    }

    override fun onDestroy() {
        renderer?.release(true)
        super.onDestroy()
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_LAMPS = "gainmaps/lamps.jpg"
    }
}