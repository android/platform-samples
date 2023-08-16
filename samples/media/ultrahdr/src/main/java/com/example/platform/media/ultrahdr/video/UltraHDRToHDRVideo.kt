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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.platform.media.ultrahdr.databinding.UltrahdrToHdrVideoBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@Sample(
    name = "UltraHDR to HDR Video",
    description = "This sample demonstrates converting a series of UltraHDR images into a HDR " +
            "video." + "The sample leverages GPU hardware acceleration to render and encode the " +
            "images.",
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
     * [ImageWriter], used to produce [Image] objects that will to convert UltraHDR image data to an
     * HDR color space (in this sample, HLG) to then be encoded by the [MediaCodec]
     */
    private lateinit var imageWriter: ImageWriter

    /**
     * The ImageWriters [HandlerThread] & [Handler], used to process
     * [ImageWriter.setOnImageReleasedListener] callbacks.
     */
    private lateinit var imageWriterHandler: Handler
    private val imageWriterThread: HandlerThread by lazy {
        HandlerThread("imageWriterThread")
    }

    /**
     * Tracks the amount for active [Image] buffers. The buffers are created using
     * [ImageWriter.dequeueInputImage].
     *
     * Using [AtomicInteger] since it can be accessed by multiple threads.
     */
    private var activeImageBuffers = AtomicInteger(0)

    /**
     * [MediaMuxer] used to save the frame from [MediaCodec] to the video file
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
     * file path for the .mp4 file to be generated too
     */
    private val filePath: String by lazy {
        folderPath + UUID.randomUUID().toString() + VIDEO_FILE_EXTENSION
    }

    /**
     * [MediaCodec] encoder that will be used to encode the the HDR video.
     */
    private lateinit var encoder: MediaCodec

    /**
     * The encoders [Handler] & [HandlerThread], used to process the [MediaCodec.Callback], which
     * process frames from the [ImageWriter]
     */
    private lateinit var encoderHandler: Handler
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
    private var framesProcessed = AtomicInteger(0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = UltrahdrToHdrVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check that hw acceleration is supported.
        if (!isHardwareAccelerationSupported())
            return displayErrorDuringSetup("HW acceleration is not supported by this device.")

        // Assure MediaCodec set up is completed.
        if (!setUpMediaCodec())
            return displayErrorDuringSetup("No capable encoder was found.")

        // Setup Image Reader.
        setUpImageWriter()

        // Attaching ColorMode Controller for Activity HDR mode for images
        binding.colorModeControls.setWindow(requireActivity().window)

        // Load UltraHDR Preview Images
        loadImageIntoView(ULTRA_HDR_IMAGES[0], binding.imageContainer1)
        loadImageIntoView(ULTRA_HDR_IMAGES[1], binding.imageContainer2)
        loadImageIntoView(ULTRA_HDR_IMAGES[2], binding.imageContainer3)

        binding.covertButton.setOnClickListener {
            binding.covertButton.isEnabled = false

            // Perform conversion task on the IO Dispatcher
            lifecycleScope.launch(Dispatchers.IO) {
                Log.i(TAG, "Starting Conversion to Video")
                doImageToVideoConversion()
            }
        }
    }

    /**
     *
     */
    private fun displayErrorDuringSetup(message: String) {
        Log.e(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        binding.covertButton.isEnabled = false
        binding.covertButton.text = message
    }

    /**
     * Checks that the device can support hardware accelerated rendering using the GPU to draw
     * UltraHDR images into [Image] [HardwareBuffer].
     */
    private fun isHardwareAccelerationSupported(): Boolean =
        HardwareBuffer.isSupported(
            FORMAT_WIDTH,                // Buffer width.
            FORMAT_HEIGHT,               // Buffer height.
            HARDWARE_BUFFER_COLOR,       // 10 bits red, 10 bits green, 10 bits blue, 2 bits alpha.
            1,                    // Number of layers.
            HARDWARE_BUFFER_USAGES,      // Required hw usage capabilities
        )


    /**
     * Sets up the [ImageWriter] to create a hardware accelerated [Image] object that contain a
     * [HardwareBuffer]. This buffer will be used with a [HardwareBufferRenderer] to draw the
     * UltraHDR images to the buffer with the correct [DataSpace]
     */
    private fun setUpImageWriter() {
        // Initialize imageWriter.
        imageWriter =
            ImageWriter.Builder(encoderSurface)
                .setHardwareBufferFormat(HARDWARE_BUFFER_COLOR)
                .setDataSpace(DataSpace.DATASPACE_BT2020_HLG)
                .setMaxImages(32)
                .setUsage(HARDWARE_BUFFER_USAGES)
                .build()

        // Start the imageWriter thread & setCallback to release available images buffers.
        imageWriterThread.start()
        imageWriterHandler = Handler(imageWriterThread.looper)
        imageWriter.setOnImageReleasedListener(
            { updateActiveImageBuffers(decrement = true) },
            Handler.createAsync(imageWriterThread.looper),
        )
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
        if (encoderName.isNullOrBlank()) return false

        // Start the MediaCodec encoder thread.
        encoderThread.start()
        encoderHandler = Handler(encoderThread.looper)

        // Initialize & configure mediaCodec.
        encoder = MediaCodec.createEncoderByType(FORMAT_MIMETYPE)
        encoder.setCallback(setUpMediaCodecCallback(), encoderHandler)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // Creating the encoder surface needed by the ImageWriter to write data to.
        encoderSurface = encoder.createInputSurface()
        encoder.start()

        return true
    }

    /**
     * The [MediaCodec.Callback], used to asynchronously process frame produced by the
     * [ImageWriter].
     */
    private fun setUpMediaCodecCallback() = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(c: MediaCodec, i: Int) {
            // When configured as an encoder, this will not be called, so do nothing.
        }

        override fun onOutputBufferAvailable(c: MediaCodec, i: Int, info: MediaCodec.BufferInfo) {
            // The first buffer available is usually the BUFFER_FLAG_CODEC_CONFIG buffer. This is
            // not a frame that contains real data to be written to the muxer, but doest contain
            // the correct media format information that will be coming subsequently. This is the
            // best time to set up the muxer with that information.
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0)
                return handleCodecConfig(c.getOutputFormat(i))

            // BUFFER_FLAG_END_OF_STREAM indicates that the stream is ended and no more buffers are
            // expected.
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                return handleEndOfStream()

            // Handle frame.
            handleFrame(c, i, info)
        }

        override fun onError(c: MediaCodec, e: MediaCodec.CodecException) {
            e.message?.let { Log.e(TAG, it) }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            // Format should not change, so do nothing.
        }
    }

    /**
     * Handles the [MediaCodec.BUFFER_FLAG_CODEC_CONFIG] case.
     *
     * We use this time to set up the [MediaMuxer] to save the frames received by [MediaCodec] to
     * an .mp4 file for playback.
     *
     * @param mediaFormat [MediaFormat] provided by the [MediaCodec]. It's important to set up the
     * [MediaMuxer] with the [MediaFormat] from the first frame of the [MediaCodec.Callback]. This
     * is because sometimes there is added information from the hardware encoder that was added
     * during [MediaCodec] after initialization.
     */
    private fun handleCodecConfig(mediaFormat: MediaFormat) {
        // Create path to cache directory. Delete any previously generated videos.
        val dir = File(folderPath)
        if (dir.exists() && dir.isDirectory) dir.listFiles()?.let {
            for (file in it) file.delete()
        }
        dir.mkdirs()

        // Initialize MediaMuxer.
        muxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxerTrackId = muxer.addTrack(mediaFormat)

        // Start MediaMuxer
        muxer.start()
    }

    /**
     * Handles the [MediaCodec.BUFFER_FLAG_END_OF_STREAM] case.
     *
     * We use this time to clean up the encoders and writers and initialize playback of the
     * generated file.
     */
    private fun handleEndOfStream() {
        Log.i(TAG, "Initiate Media and ImageWriter Cleanup.")

        // clear up resources
        muxer.stop()
        encoder.release()
        imageWriter.close()

        // Initialize ExoPlayer to playback video
        Log.i(TAG, "Initiate playback using ExoPlayer.")
        lifecycleScope.launch { playbackUsingExoPlayer() }
    }

    private fun handleFrame(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) =
        try {
            // Start frame buffer processing.
            isFrameBufferProcessed.set(false)

            // Retrieve the latest buffer. This can be null and should be skipped if so
            codec.getOutputBuffer(index)?.let {
                // Write the frame to the muxer.
                muxer.writeSampleData(muxerTrackId, it, info)

                // Release buffer, there is not output surface so no need to render to it.
                codec.releaseOutputBuffer(index, false)
            }
        } catch (e: Exception) {
            val message = "Error in processing frame: ${e.message}, dropping frame..."
            Log.e(TAG, message)
        } finally {
            isFrameBufferProcessed.set(true)
            updateFramesProcessed()
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
     * Sets up an [ExoPlayer] instance to playback the rendered file. [ExoPlayer] under the hood
     * will automatically detect that the file is HDR and activate high brightness mode on the
     * device for HDR playback.
     */
    private suspend fun playbackUsingExoPlayer() = withContext(Dispatchers.Main) {
        binding.mediaPlayer.useController = false

        val player = ExoPlayer.Builder(requireContext()).build()
        player.setMediaItem(MediaItem.fromUri(filePath))
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.prepare()

        // Attaching player to player view
        binding.mediaPlayer.player = player

        // Play back video
        player.play()
    }

    /**
     * Loads [Bitmap] into memory to then render to the [ImageWriter].
     */
    private suspend fun doImageToVideoConversion() = withContext(Dispatchers.IO) {
        // Preload image bitmaps into memory. We are doing this mainly to avoid not providing the
        // MediaEncoder a consistent stream of frames dues to the loading and scaling time needed
        // for bitmaps
        val bitmaps = Array(ULTRA_HDR_IMAGES.size) {
            val stream = context?.assets?.open(ULTRA_HDR_IMAGES[it])
            BitmapFactory.decodeStream(stream)
        }

        bitmaps.forEach { bitmap ->
            var frameCount = 1
            while (frameCount <= FORMAT_FRAME_RATE) {
                // Assure frame has been processed and max dequeued image buffers have not been
                // supposed.
                val isFrameProcessed = isFrameBufferProcessed.acquire
                val isOverMaxImagesLimit = activeImageBuffers.acquire >= imageWriter.maxImages
                if (!isFrameProcessed || isOverMaxImagesLimit) continue

                // Get a reference to an Image from the ImageWriter.
                val image = imageWriter.dequeueInputImage()
                updateActiveImageBuffers()

                // Render Bitmap content to Image hardware buffer.
                renderBitmapToImageViaHardware(
                    bitmap,
                    image,
                    ColorSpace.get(ColorSpace.Named.BT2020_HLG),
                )

                // await until HardwareBufferRenderer.RenderResult fence is set on the image.
                while (!image.fence.isValid) { /* await */
                }
                image.fence.awaitForever()
                imageWriter.queueInputImage(image)

                // Increase frame count.
                frameCount++
            }
        }

        // Processed has completed. Now we signal the encoder that the we no longer have any buffers
        // and it should clean up.
        encoder.signalEndOfInputStream()
    }

    /**
     * Renders [Bitmap] contents to an [Image]'s [HardwareBuffer].
     */
    private suspend fun renderBitmapToImageViaHardware(
        bitmap: Bitmap,
        image: Image,
        dest: ColorSpace,
    ) = withContext(Dispatchers.Default) {
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
                { result -> image.fence = result.fence },
            )
        }
    }

    /**
     * Updates the "Frames Processed" text used to indicate the amount of frame that the
     * [MediaCodec.Callback.onOutputBufferAvailable] has processed and written to the file via
     * [MediaMuxer.writeSampleData] function.
     */
    private fun updateFramesProcessed() {
        val processed = framesProcessed.incrementAndGet()
        val processedOverTotal = "$processed/${FORMAT_FRAME_RATE * ULTRA_HDR_IMAGES.size}"
        val status = "Frames Processed: $processedOverTotal"
        Log.i(TAG, status)

        // Update UI
        lifecycleScope.launch { binding.statusFramesProcessed.text = status }
    }

    /**
     * Updates the "Active Image Buffers" text. This is called for every
     * [ImageWriter.dequeueInputImage] (increments) and evert
     * [ImageWriter.setOnImageReleasedListener] callback (decrements).
     *
     * @param decrement Whether or not the value should decrement.
     */
    private fun updateActiveImageBuffers(decrement: Boolean = false) {
        val activeImageBuffers = when (decrement) {
            true -> activeImageBuffers.decrementAndGet()
            false -> activeImageBuffers.incrementAndGet()
        }
        val status = "Active Image Buffers: $activeImageBuffers"

        // Update UI
        lifecycleScope.launch { binding.activeImageBuffers.text = status }
    }

    /**
     * Loads images into ImageView using [Glide].
     *
     * @param path Path to image file.
     * @param view [ImageView] file will be loaded into.
     */
    private fun loadImageIntoView(path: String, view: ImageView) =
        Glide.with(this)
            .load(Uri.parse("file:///android_asset/$path"))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)

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
         * Sample UltraHDR images paths
         */
        private val ULTRA_HDR_IMAGES = arrayListOf(
            "gainmaps/desert_palms.jpg",
            "gainmaps/desert_sunset.jpg",
            "gainmaps/desert_wanda.jpg",
        )

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
        private const val FORMAT_FRAME_RATE = 60

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
    }
}