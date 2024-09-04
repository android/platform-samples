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
import android.graphics.ColorSpace
import android.graphics.Typeface
import android.hardware.DataSpace
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.opengl.FrameBuffer
import androidx.graphics.opengl.GLFrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.SyncFenceCompat
import com.example.platform.graphics.ultrahdr.R
import com.example.platform.graphics.ultrahdr.databinding.UltrahdrWithGraphicsBinding
import com.google.android.catalog.framework.annotations.Sample
import java.util.function.Consumer

@Sample(
    name = "UltraHDR x OpenGLES SurfaceView",
    description = "This sample demonstrates displaying an UltraHDR image via and OpenGL Pipeline " +
            "and control the SurfaceView's rendering brightness.",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class UltraHDRWithOpenGL : Fragment(),
    GLRenderer.EGLContextCallback,
    GLFrameBufferRenderer.Callback {
    /**
     * ExtendedBrightnessValue enum to control
     */
    private enum class ExtendedBrightnessValue(val value: Int) {
        ZERO_PERCENT(0),
        THIRTY_PERCENT(1),
        SEVENTY_PERCENT(2),
        ONE_HUNDRED_PERCENT(3);

        fun toFloatValue() = when (this) {
            ZERO_PERCENT -> 0.0f
            THIRTY_PERCENT -> 0.3f
            SEVENTY_PERCENT -> 0.7f
            ONE_HUNDRED_PERCENT -> 1.0f
        }

        companion object {
            fun fromInt(value: Int) = ExtendedBrightnessValue.values().first { it.value == value }
        }
    }

    /**
     *  Android ViewBinding.
     */
    private var _binding: UltrahdrWithGraphicsBinding? = null
    private val binding get() = _binding!!

    /**
     * Is the device screen wide gamut.
     */
    private var _isWideGamut: Boolean? = null
    private val isWideGamut get() = _isWideGamut!!

    private lateinit var glRenderer: GLRenderer
    private lateinit var glFrameBufferRenderer: GLFrameBufferRenderer
    private lateinit var ultraHDRGLRenderer: UltraHDRWithOpenGLRenderer

    private var hdrSdrRatio = 1.0f
    private var desiredRatio = 1.0f

    private lateinit var bitmap: Bitmap
    private val updateHdrSdrRatio = Consumer<Display> { glFrameBufferRenderer.render() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UltrahdrWithGraphicsBinding.inflate(inflater, container, false)
        _isWideGamut = requireContext().display?.isWideColorGamut == true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stream = context?.assets?.open(ULTRA_HDR_IMAGE)
        bitmap = BitmapFactory.decodeStream(stream)

        // Initialize GL pipeline when surface is created.
        initUltraHDRGlRenderer()
        initGlRenderer()
        initGlFrameRenderer()

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.colorModeControls.setWindow(requireActivity().window)
        binding.colorModeControls.binding.ultrahdrColorModeHdr.visibility = View.GONE
        binding.colorModeControls.binding.ultrahdrColorModeSdr.visibility = View.GONE
        binding.openglSurfaceViewExtendedBrightness.typeface = Typeface.DEFAULT_BOLD
        binding.openglBrightnessGroup.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            val index = group.indexOfChild(selected)
            val ebv = ExtendedBrightnessValue.fromInt(index)
            val ebValue = ebv.toFloatValue()
            desiredRatio = maxOf(1.0f, ultraHDRGLRenderer.desiredHdrSdrRatio * ebValue)

            binding.openglSurfaceViewExtendedBrightness.text = String.format(
                resources.getString(R.string.ultrahdr_with_opengl_surface_view_extended_brightness),
                desiredRatio,
            )

            // TODO Android U doesn't react well when desiredRatio changes from anything other than
            //      1.0f. So for now, we will force to go back to 1.0f (0%) and then unlock other
            //      options
            updateAvailableBrightnessOptions(ebv)
            glFrameBufferRenderer.render()
        }

        // Set Initial Render brightness to 0%
        binding.openglBrightness0.isChecked = true
    }

    private fun updateAvailableBrightnessOptions(ebv: ExtendedBrightnessValue) {
        when (ebv) {
            ExtendedBrightnessValue.ZERO_PERCENT -> {
                binding.openglBrightness30.isEnabled = true
                binding.openglBrightness70.isEnabled = true
                binding.openglBrightness100.isEnabled = true
            }

            else -> {
                binding.openglBrightness30.isEnabled = false
                binding.openglBrightness70.isEnabled = false
                binding.openglBrightness100.isEnabled = false
            }
        }
    }

    private fun initUltraHDRGlRenderer() {
        ultraHDRGLRenderer = UltraHDRWithOpenGLRenderer(requireContext(), bitmap)
        desiredRatio = ultraHDRGLRenderer.desiredHdrSdrRatio
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
        requireActivity().display?.let { display ->
            if (display.isHdrSdrRatioAvailable) {
                display.registerHdrSdrRatioChangedListener(Runnable::run, updateHdrSdrRatio)
            }
        }
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
        requireActivity().display
            ?.unregisterHdrSdrRatioChangedListener(updateHdrSdrRatio)
    }

    override fun onDestroyView() {
        glFrameBufferRenderer.release(true)
        super.onDestroyView()
    }

    override fun onEGLContextCreated(eglManager: EGLManager) {
        val colorSpaceName = when (isWideGamut) {
            true -> ColorSpace.Named.DISPLAY_P3
            false -> ColorSpace.Named.SRGB
        }

        ultraHDRGLRenderer.onContextCreated(ColorSpace.get(colorSpaceName))
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
        transaction.setDataSpace(targetSurfaceControl, if (isWideGamut) P3_XRB else SRGB_XRB)
        transaction.setExtendedRangeBrightness(targetSurfaceControl, hdrSdrRatio, desiredRatio)
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE = "gainmaps/lamps.jpg"

        /**
         * DataSpace of SRGB_ERB.
         */
        private val SRGB_XRB = DataSpace.pack(
            DataSpace.STANDARD_BT709,
            DataSpace.TRANSFER_SRGB,
            DataSpace.RANGE_EXTENDED,
        )

        /**
         * DataSpace of P3_XRB.
         */
        private val P3_XRB = DataSpace.pack(
            DataSpace.STANDARD_DCI_P3,
            DataSpace.TRANSFER_SRGB,
            DataSpace.RANGE_EXTENDED,
        )
    }
}