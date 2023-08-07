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

package com.example.platform.media.ultrahdr.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.HardwareBufferRenderer
import android.graphics.RenderNode
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageWriter
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asImageBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.media.ultrahdr.databinding.UltrahdrToHdrVideoBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch


@Sample(
    name = "UltraHDR to HDR Video",
    description = "This sample demonstrates converting a series of UltraHDR images into a " +
            "10-bit HDR video",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class UltraHDRToHDRVideo : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: UltrahdrToHdrVideoBinding? = null
    private val binding get() = _binding!!

    /**
     * [MediaCodec] encoder that will be used to encode the video.
     */
    private val encoder: MediaCodec by lazy {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            1920,
            1080,
        )

        val codecName = MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(format)
        MediaCodec.createEncoderByType(codecName)
    }

    /**
     * List for UltraHDR Images
     */
    private val ultraHDRImages = listOf(
        ULTRA_HDR_IMAGE_1,
        ULTRA_HDR_IMAGE_2,
        ULTRA_HDR_IMAGE_3,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UltrahdrToHdrVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.colorModeControls.setWindow(requireActivity().window)
        binding.covertButton.setOnClickListener {
            convertImagesToVideo()
        }

        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_2)
        val bitmap = BitmapFactory.decodeStream(stream)

        binding.imageContainer.setImageBitmap(bitmap)
    }

    private fun convertImagesToVideo() = lifecycleScope.launch(Dispatchers.Main) {
        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_1)
        val bitmap = BitmapFactory.decodeStream(stream)

        val surface = encoder.createInputSurface()
        val writer = ImageWriter.Builder(surface)
            .setHardwareBufferFormat(HardwareBuffer.RGBA_1010102)
            .setDataSpace(DataSpace.DATASPACE_BT2020_HLG)
            .build()

        val image = writer.dequeueInputImage()

        val render =
            renderImageFrameWithHardware(bitmap, image, ColorSpace.get(ColorSpace.Named.BT2020_HLG))

        binding.imageContainer.setImageBitmap(render)
    }

    private suspend fun renderImageFrameWithHardware(
        bitmap: Bitmap,
        image: Image,
        dest: ColorSpace,
    ): Bitmap = withContext(Dispatchers.IO) {
        val renderer = HardwareBufferRenderer(image.hardwareBuffer!!)
        val content = RenderNode("frame")
        content.setPosition(0, 0, bitmap.width, bitmap.height)

        val canvas = content.beginRecording()
        canvas.drawBitmap(bitmap, .0f, .0f, null)
        content.endRecording()

        renderer.setContentRoot(content)
        renderer.obtainRenderRequest()
            .setColorSpace(dest)
            .draw(
                { runnable -> runnable.run() },
            ) { result -> result.fence.awaitForever() }

        return@withContext Bitmap.wrapHardwareBuffer(image.hardwareBuffer!!, dest)!!
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Tag
         */
        private val TAG = UltraHDRToHDRVideo::class.java.simpleName

        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_1 = "gainmaps/desert_palms.jpg"
        private const val ULTRA_HDR_IMAGE_2 = "gainmaps/desert_sunset.jpg"
        private const val ULTRA_HDR_IMAGE_3 = "gainmaps/desert_wanda.jpg"
    }
}