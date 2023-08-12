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
import android.graphics.HardwareBufferRenderer.RenderResult
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
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

@Sample(
    name = "UltraHDR to HDR Video",
    description = "This sample demonstrates converting a series of UltraHDR images into a HDR " +
            "video." + "The sample leverages GPU hardware acceleration to render and encode the " +
            "images",
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
     * The ImageWriters [Handler], used to process [ImageWriter.setOnImageReleasedListener]
     * callbacks
     */
    private lateinit var imageWriterHandler: Handler

    /**
     * The ImageWriters [HandlerThread].
     */
    private val imageWriterThread: HandlerThread by lazy {
        HandlerThread("imageWriterThread")
    }

    /**
     * Flag to determine whether to [Image] that was dequeued from the
     * [ImageWriter.dequeueInputImage] has been released.
     *
     * Using [AtomicBoolean] since it can be accessed by multiple threads.
     */
    private var isImageReleased = AtomicBoolean(true)

    /**
     * [MediaMuxer] used to save the frame from [MediaCodec] to the disk
     */
    private lateinit var muxer: MediaMuxer

    /**
     * [MediaMuxer] track id, to identify which track is the encoding track.
     */
    private var muxerTrackId = -1

    /**
     * Folder path for the .mp4 files to be generated too
     */
    private val folderPath: String by lazy {
        requireActivity().cacheDir.path + '/' + FOLDER_PATH_NAME + '/'
    }

    /**
     * [MediaCodec] encoder that will be used to encode the the HDR 10-bit video.
     */
    private lateinit var encoder: MediaCodec

    /**
     * The encoders [Handler], used to process the encoders [MediaCodec.Callback] asynchronously.
     */
    private lateinit var encoderHandler: Handler

    /**
     * The encoders [HandlerThread].
     */
    private val encoderThread: HandlerThread by lazy {
        HandlerThread("encoderThread")
    }

    /**
     * [Surface], produced from [MediaCodec] that will, that is used by the [ImageWriter] to fill
     * with image data to be encoded by the encoder.
     */
    private lateinit var encoderSurface: Surface

    /**
     * Flag to determine whether or not the frame has been processed by the [MediaCodec] & written
     * to the [MediaMuxer].
     *
     * Using [AtomicBoolean] since it can be accessed by multiple threads.
     */
    private var isFrameBufferProcessed = AtomicBoolean(true)

    /**
     * TBD
     */
    private val encoderCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // When configured as an encoder, this will not be called, so do nothing.
        }

        override fun onOutputBufferAvailable(c: MediaCodec, i: Int, info: MediaCodec.BufferInfo) {
            // First check if the flags contain BUFFER_FLAG_END_OF_STREAM. This indicates that the
            // stream is ended and no more buffers are expected.
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.i(TAG, "signalEndOfInputStream() Called, initiate cleanup")

                // Stop the muxer & release the encoder
                muxer.stop()
                encoder.release()
                return
            }

            // Retrieve the latest buffer. This can be null and should be skipped if so
            val buffer = c.getOutputBuffer(i) ?: return

            // Start frame buffer processing.
            isFrameBufferProcessed.set(false)

            // Set up the muxer to write with the first frame processed from the codec. Check the
            // setUpMediaMuxer() function for more details.
            if (muxerTrackId == -1) setUpMediaMuxer(c.getOutputFormat(i))

            // Write the frame to the muxer.
            muxer.writeSampleData(muxerTrackId, buffer, info)

            // Release buffer, freeing memory
            c.releaseOutputBuffer(i, false)

            // Set frame processed.
            isFrameBufferProcessed.set(true)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            e.message?.let { Log.e(TAG, it) }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            // Format should not change, so do nothing.
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = UltrahdrToHdrVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check that hw acceleration is supported
        if (!isHardwareAccelerationSupported()) return
        if (!setUpMediaCodec()) return
        if (!setUpImageWriter()) return

        binding.covertButton.setOnClickListener {
            binding.covertButton.isEnabled = false
            initializeConversionToVideo()
        }
    }

    /**
     * Assure device can support hardware accelerated rendering using the GPU to draw UltraHDR
     * images into HDR frames for the [MediaCodec].
     */
    private fun isHardwareAccelerationSupported(): Boolean = HardwareBuffer.isSupported(
        FORMAT_WIDTH,                // Buffer width.
        FORMAT_HEIGHT,               // Buffer height.
        HARDWARE_BUFFER_COLOR,       // 10 bits red, 10 bits green, 10 bits blue, 2 bits alpha.
        1,                    // Number of layers.
        HARDWARE_BUFFER_USAGES,      // Required hw usage capabilities
    )

    /**
     * Sets up the muxer to save the frames received by [MediaCodec] to an .mp4 file for playback.
     *
     * @param mediaFormat [MediaFormat] provided by the [MediaCodec]. It's important to set up the
     * [MediaMuxer] with the [MediaFormat] from the first frame of the [MediaCodec.Callback]. This
     * is because sometimes there is added information from the hardware encoder that was added
     * during [MediaCodec] initialization.
     */
    private fun setUpMediaMuxer(mediaFormat: MediaFormat) {
        // Create path to cache directory. Delete any previously generated videos.
        val dir = File(folderPath)
        if (dir.exists() && dir.isDirectory) dir.listFiles()?.let {
            for (file in it) file.delete()
        }

        dir.mkdirs()
        val path = folderPath + UUID.randomUUID().toString() + VIDEO_FILE_EXTENSION

        // Initialize MediaMuxer.
        muxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxerTrackId = muxer.addTrack(mediaFormat)

        // Start  MediaMuxer
        muxer.start()
    }

    /**
     * Sets up the encoder to produce a 10-bit HDR video. The devices encoder must support
     * [MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10]. We will check for that first before
     * proceeding with initialization of the [MediaCodec].
     *
     * @return true if set up was successful, otherwise false.
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
        encoder = MediaCodec.createEncoderByType(FORMAT_MIMETYPE)
        encoder.setCallback(encoderCallback, encoderHandler)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // Creating the encoder surface needed by the ImageWriter to write data to.
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
            setInteger(MediaFormat.KEY_MAX_FPS_TO_ENCODER, FORMAT_FRAME_RATE)
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
                .setHardwareBufferFormat(HARDWARE_BUFFER_COLOR)
                .setDataSpace(DataSpace.DATASPACE_BT2020_HLG)
                .setMaxImages(2)
                .setUsage(HARDWARE_BUFFER_USAGES)
                .build()

        // Start the imageWriter thread.
        imageWriterThread.start()
        imageWriterHandler = Handler(imageWriterThread.looper)
        imageWriter.setOnImageReleasedListener(
            { isImageReleased.set(true) },
            imageWriterHandler,
        )

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
                // Wait until Image from ImageWriter has been released and the frame has been
                // processed.
                if (!isFrameBufferProcessed.get() || !isImageReleased.get()) {
                    continue
                }

                isImageReleased.set(false)

                // Get a reference to an Image from the ImageWriter.
                val image = imageWriter.dequeueInputImage()

                // Initiate A GPU color space conversion using the UltraHDR image
                renderImageFrameWithHardware(bitmap, image, colorSpace)

                // Increase frame count.
                frameCount++
            }
        }

        encoder.signalEndOfInputStream()
    }

    private fun renderImageFrameWithHardware(bitmap: Bitmap, image: Image, dest: ColorSpace) {
        image.hardwareBuffer?.let { buffer ->
            // Initialize HardwareBufferRenderer
            val renderer = HardwareBufferRenderer(buffer)

            // Initialize & Configure RenderNode.
            val node = RenderNode("ultra-hdr-to-video")
            node.setPosition(0, 0, image.width, image.height)

            // Draw the bitmap contents onto the render node.
            val canvas = node.beginRecording()
            canvas.drawBitmap(bitmap, .0f, .0f, null)
            node.endRecording()

            // Set render node to hardware renderer
            renderer.setContentRoot(node)

            // Render the nodes contents to the hardware buffer of the provided image hardware
            // buffer.
            renderer.obtainRenderRequest().setColorSpace(dest).draw(
                { exe -> exe.run() },
                { result: RenderResult ->
                    result.fence.awaitForever()
                    imageWriter.queueInputImage(image)
                },
            )
        }
    }

    companion object {
        /**
         * Class Tag
         */
        private val TAG = UltraHDRToHDRVideo::class.java.simpleName

        /**
         * Folder path name where .mp4 files will be encoded to
         */
        private val FOLDER_PATH_NAME = UltraHDRToHDRVideo::class.java.simpleName

        /**
         * File Extension
         */
        private const val VIDEO_FILE_EXTENSION = ".mp4"

        /**
         * The group of [HardwareBuffer] usages that are needed in order to process the frame via
         * the GPU hardware.
         *
         * * USAGE_GPU_COLOR_OUTPUT ->  The buffer will be written to by the hardware GPU.
         * * USAGE_GPU_SAMPLED_IMAGE -> The buffer will be read from by the GPU
         */
        private const val HARDWARE_BUFFER_USAGES =
            HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE

        /**
         * The required [HardwareBuffer] color needed to produce 10-bit RGB data
         */
        private const val HARDWARE_BUFFER_COLOR = HardwareBuffer.RGBA_1010102

        /**
         * [MediaFormat] Parameters, used to configure [MediaCodec] to produce HDR Frames
         */
        // HEVC Mimetype.
        private const val FORMAT_MIMETYPE = MediaFormat.MIMETYPE_VIDEO_HEVC

        // Indicates that the data will be A GraphicBuffer metadata reference.
        private const val FORMAT_COLOR_FORMAT =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

        // HEVC/H.265 Main 10 Profile. Works for HLG
        // Change to HEVCProfileMain10HDR10 for PQ
        private const val FORMAT_PROFILE = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10

        // 30 MBPS.
        private const val FORMAT_BITRATE = 30 * 1000000

        // 30 Frames Per Second.
        private const val FORMAT_FRAME_RATE = 30

        // Number of partial frames that occur between full frames in the video stream. Set to -1 to
        // assure all frames are full-frames.
        private const val FORMAT_I_FRAME_INTERVAL = -1

        // ITU-R Recommendation BT.2020 color primaries.
        private const val FORMAT_COLOR_STANDARD = MediaFormat.COLOR_STANDARD_BT2020

        // Full range YCrCb component values
        private const val FORMAT_COLOR_RANGE = MediaFormat.COLOR_RANGE_FULL

        // Hybrid-log-gamma transfer function.
        // Change to COLOR_TRANSFER_ST2084 for PQ
        private const val FORMAT_COLOR_TRANSFER = MediaFormat.COLOR_TRANSFER_HLG

        // Video width
        private const val FORMAT_WIDTH = 3840

        // Video height
        private const val FORMAT_HEIGHT = 2160

        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_1 = "gainmaps/desert_palms.jpg"
        private const val ULTRA_HDR_IMAGE_2 = "gainmaps/desert_sunset.jpg"
        private const val ULTRA_HDR_IMAGE_3 = "gainmaps/desert_wanda.jpg"
    }
}