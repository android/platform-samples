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
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.platform.media.ultrahdr.databinding.UltrahdrToHdrVideoBinding
import com.google.android.catalog.framework.annotations.Sample
import java.util.UUID


@Sample(
    name = "UltraHDR to HDR Video",
    description = "This sample demonstrates converting a series of UltraHDR images into a " + "10-bit HDR video using Hardware Acceleration",
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
     * [ImageWriter], used to produce [Image] object that will to convert UltraHDR image data to an
     * HDR color space (in this sample, HLG) to then be encoded by the [MediaCodec]
     */
    private lateinit var imageWriter: ImageWriter

    /**
     *
     */
    private var isImageReleased = true
    private var isFrameProcessed = true

    /**
     * [MediaMuxer] used to save the frame from [MediaCodec] to the disk
     */
    private lateinit var muxer: MediaMuxer

    /**
     * [MediaCodec] encoder that will be used to encode the the HDR 10-bit video.
     */
    private lateinit var encoder: MediaCodec

    /**
     * The encoders [Handler], used to process the encoders [MediaCodec.Callback] asynchronously.
     */
    private lateinit var encoderHandler: Handler

    /**
     * The encoders [HandlerThread],
     */
    private val encoderThread: HandlerThread by lazy {
        HandlerThread("encoderThread-" + UUID.randomUUID().toString())
    }

    /**
     * [Surface], produced from [MediaCodec] that will, that is used by the [ImageWriter] to fill
     * with image data to be encoded by the encoder.
     */
    private lateinit var encoderSurface: Surface

    /**
     *
     */
    private val encoderCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {/* Do nothing. */
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo,
        ) {
            val outputBuffer = codec.getOutputBuffer(index)
            val bufferFormat = codec.getOutputFormat(index)

            // Insert frame into muxer
            muxer.writeSampleData(0, outputBuffer!!, info)

            codec.releaseOutputBuffer(index, false)
            isFrameProcessed = true
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            e.message?.let { Log.e(TAG, it) }
        }

        override fun onOutputFormatChanged(
            codec: MediaCodec,
            format: MediaFormat,
        ) {/* Do nothing. */
        }
    }

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

        // Check that hw acceleration is supported
        if (!isHardwareAccelerationSupported()) return
        if (!setUpMediaCodec()) return
        if (!setUpMediaMuxer()) return
        if (!setUpImageWriter()) return

        binding.colorModeControls.setWindow(requireActivity().window)
        binding.covertButton.setOnClickListener {
            binding.covertButton.isEnabled = false
            initializeConversionToVideo()
        }

        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_2)
        val bitmap = BitmapFactory.decodeStream(stream)

        binding.imageContainer.setImageBitmap(bitmap)
    }

    /**
     * Assure device can support hardware accelerated rendering using the GPU to draw UltraHDR
     * images into HDR frames for the [MediaCodec].
     */
    private fun isHardwareAccelerationSupported(): Boolean = HardwareBuffer.isSupported(
        // Buffer width.
        FORMAT_WIDTH,

        // Buffer height.
        FORMAT_HEIGHT,

        // 10 bits red, 10 bits green, 10 bits blue, 2 bits alpha.
        HardwareBuffer.RGBA_1010102,

        // 1 layer.
        1,

        // USAGE_GPU_COLOR_OUTPUT = The buffer will be written to by the hardware GPU.
        // USAGE_VIDEO_ENCODE = The buffer will be read by a hardware video encoder.
        HardwareBuffer.USAGE_GPU_COLOR_OUTPUT and HardwareBuffer.USAGE_VIDEO_ENCODE,
    )

    /**
     * Sets up the muxer to save the frames received by [MediaCodec] to an .mp4 container for
     * device playback.
     */
    private fun setUpMediaMuxer(): Boolean {
        // Create path to cache directory.
        val path = requireActivity().cacheDir.path + '/' + UUID.randomUUID().toString() + ".mp4"

        // Initialize Media muxer.
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer.addTrack(encoder.outputFormat)
        muxer.start()
        return true
    }

    /**
     * Sets up the encoder to produce a 10-bit HDR video. The devices encoder must support
     * [MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10]. We will check for that first before
     * proceeding with initialization of the [MediaCodec]
     */
    private fun setUpMediaCodec(): Boolean {
        // Create the HDR MediaFormat that will be used to create our HDR encoder
        val format = createHdrMediaFormat()

        // Before initializing the encoder, we must first check if the encoder is supported by the
        // device as not every device will have the ability to encode 10-bit HDR.
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val encoderName = codecList.findEncoderForFormat(format)
        if (encoderName.isNullOrBlank()) {
            val message = "No capable encoder was found. Terminating setup."
            Log.e(TAG, message)
            return false
        }

        // Start the MediaCodec encoder thread.
        encoderThread.start()
        encoderHandler = Handler(encoderThread.looper)

        // Initialize & configure mediaCodec.
        encoder = MediaCodec.createByCodecName(encoderName)
        encoder.setCallback(encoderCallback, encoderHandler)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoderSurface = encoder.createInputSurface()
        encoder.start()
        return true
    }

    /**
     * Create a [MediaFormat] with the necessary parameter to produce a 10-bit HDR video. The
     * [MediaFormat] parameter specifics will are in the companion object.
     */
    private fun createHdrMediaFormat(): MediaFormat =
        MediaFormat.createVideoFormat(FORMAT_MIMETYPE, FORMAT_WIDTH, FORMAT_HEIGHT).apply {
            setInteger(MediaFormat.KEY_PROFILE, FORMAT_PROFILE)
            setInteger(MediaFormat.KEY_BIT_RATE, FORMAT_BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FORMAT_FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FORMAT_I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, FORMAT_COLOR_FORMAT)
            setInteger(MediaFormat.KEY_COLOR_STANDARD, FORMAT_COLOR_STANDARD)
            setInteger(MediaFormat.KEY_COLOR_RANGE, FORMAT_COLOR_RANGE)
            setInteger(MediaFormat.KEY_COLOR_TRANSFER, FORMAT_COLOR_TRANSFER)
        }

    /**
     * Sets up the [ImageWriter] to create a hardware accelerated [Image] object that contain a
     * [HardwareBuffer]. This buffer will be used with a [HardwareBufferRenderer] to draw the
     * UltraHDR images to the buffer with the correct [DataSpace]
     */
    private fun setUpImageWriter(): Boolean {
        // Initialize imageWriter.
        imageWriter =
            ImageWriter.Builder(encoderSurface)
                .setHardwareBufferFormat(HardwareBuffer.RGBA_1010102)
                .setDataSpace(DataSpace.DATASPACE_BT2020_HLG)
                .setUsage(HardwareBuffer.USAGE_GPU_COLOR_OUTPUT and HardwareBuffer.USAGE_VIDEO_ENCODE)
                .build()
                .apply {
                    setOnImageReleasedListener(
                        {
                            Log.d(TAG, "setOnImageReleasedListener() Called")
                            isImageReleased = true
                        },
                        encoderHandler,
                    )
                }
        return true
    }

    private fun initializeConversionToVideo() {
        arrayListOf(
            ULTRA_HDR_IMAGE_1,
            ULTRA_HDR_IMAGE_2,
            ULTRA_HDR_IMAGE_3,
        ).forEach { path ->
            val stream = context?.assets?.open(path)
            val bitmap = BitmapFactory.decodeStream(stream)
            val colorSpace = ColorSpace.get(ColorSpace.Named.BT2020_HLG)

            var frameCount = 0
            while (frameCount < FORMAT_FRAME_RATE) {
                if (!isImageReleased || !isFrameProcessed) continue
                isImageReleased = false
                isFrameProcessed = false

                // Get a reference to an Image from the ImageWriter.
                // Assure Image sync fence is completed before attempting to write to it's buffer.
                val hwImage = imageWriter.dequeueInputImage()
                hwImage.fence.awaitForever()

                // Initiate A GPU color space conversion using the UltraHDR image
                renderImageFrameWithHardware(bitmap, hwImage, colorSpace)

                // Increase frame count.
                frameCount++
            }
        }

        muxer.stop()
        muxer.release()
        encoder.stop()
        encoder.release()
    }

    private fun renderImageFrameWithHardware(
        bitmap: Bitmap,
        image: Image,
        dest: ColorSpace,
    ) {
        val content = RenderNode("node")
        content.setPosition(0, 0, image.width, image.height)

        val renderer = HardwareBufferRenderer(image.hardwareBuffer!!)
        renderer.setContentRoot(content)

        val canvas = content.beginRecording()
        canvas.drawBitmap(bitmap, .0f, .0f, null)
        content.endRecording()

        renderer.obtainRenderRequest()
            .setColorSpace(dest)
            .draw(
                { executor -> executor.run() },
                {
                    image.fence = it.fence
                    imageWriter.queueInputImage(image)
                },
            )
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Class Tag
         */
        private val TAG = UltraHDRToHDRVideo::class.java.simpleName

        /**
         * [MediaFormat] Parameters, used to configure [MediaCodec] to produce A 10-HDR video
         */
        // HEVC Mimetype.
        private const val FORMAT_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_HEVC

        // Indicates that the data will be a GraphicBuffer metadata reference.
        private const val FORMAT_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

        // HEVC/H.265 Main 10 Profile.
        private const val FORMAT_PROFILE = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10

        // 9 MBPS.
        private const val FORMAT_BITRATE = 9000000

        // 30 Frames Per Second.
        private const val FORMAT_FRAME_RATE = 30

        // Number of partial frames that occur between full frames in the video stream. Set to 0 to
        // assure all frames are full-frames.
        private const val FORMAT_I_FRAME_INTERVAL = 0

        // ITU-R Recommendation BT.2020 color primaries.
        private const val FORMAT_COLOR_STANDARD = MediaFormat.COLOR_STANDARD_BT2020

        // Full range YCrCb component values
        private const val FORMAT_COLOR_RANGE = MediaFormat.COLOR_RANGE_FULL

        // Hybrid-log-gamma transfer function. used to create HDR content
        private const val FORMAT_COLOR_TRANSFER = MediaFormat.COLOR_TRANSFER_HLG

        // Video width
        private const val FORMAT_WIDTH = 1920

        // Video height
        private const val FORMAT_HEIGHT = 1080

        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_1 = "gainmaps/desert_palms.jpg"
        private const val ULTRA_HDR_IMAGE_2 = "gainmaps/desert_sunset.jpg"
        private const val ULTRA_HDR_IMAGE_3 = "gainmaps/desert_wanda.jpg"
    }
}