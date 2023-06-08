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

package com.example.platform.camera.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.platform.camera.databinding.FragmentImageViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import kotlin.math.max

class AdvancedImageViewer() : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: FragmentImageViewerBinding? = null
    private val binding get() = _binding!!

    /**
     * Default Bitmap decoding options
     */
    private val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        // Keep Bitmaps at less than 1 MP
        if (max(outHeight, outWidth) > DOWN_SAMPLE_SIZE) {
            val scaleFactorX = outWidth / DOWN_SAMPLE_SIZE + 1
            val scaleFactorY = outHeight / DOWN_SAMPLE_SIZE + 1
            inSampleSize = max(scaleFactorX, scaleFactorY)
        }
    }

    /** Bitmap transformation derived from passed arguments */
    private val bitmapTransformation: Matrix by lazy {
        decodeExifOrientation(requireArguments().getInt(ARG_KEY_ORIENTATION))
    }

    /**
     * Whether or not the resulting image has depth data or not.
     */
    private val isDepth: Boolean by lazy {
        requireArguments().getBoolean(ARG_KEY_IS_DEPTH)
    }

    /**
     * Location of the image file to load.
     */
    private val location: String by lazy {
        requireArguments().getString(ARG_KEY_LOCATION, "")
    }

    /** Data backing our Bitmap viewpager */
    private val bitmapList: MutableList<Bitmap> = mutableListOf()

    private fun imageViewFactory() = ImageView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentImageViewerBinding.inflate(inflater, container, false)
        binding.fragmentImageViewerViewpager2.apply {
            // Populate the ViewPager and implement a cache of two media items
            offscreenPageLimit = 2
            adapter = GenericListAdapter(
                bitmapList,
                itemViewFactory = { imageViewFactory() },
            ) { view, item, _ ->
                (view as ImageView).load(item) {
                    crossfade(true)
                }
            }
        }

        binding.fragmentImageViewerBack.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .remove(this)
                .commit()
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {

            val pager = binding.fragmentImageViewerViewpager2
            // Load input image file
            val inputBuffer = loadInputBuffer()

            // Load the main JPEG image
            addItemToViewPager(pager, decodeBitmap(inputBuffer, 0, inputBuffer.size))

            // If we have depth data attached, attempt to load it
            if (isDepth) {
                try {
                    val depthStart = findNextJpegEndMarker(inputBuffer, 2)
                    addItemToViewPager(
                        pager,
                        decodeBitmap(
                            inputBuffer, depthStart, inputBuffer.size - depthStart,
                        ),
                    )

                    val confidenceStart = findNextJpegEndMarker(inputBuffer, depthStart)
                    addItemToViewPager(
                        pager,
                        decodeBitmap(
                            inputBuffer, confidenceStart, inputBuffer.size - confidenceStart,
                        ),
                    )

                } catch (exc: RuntimeException) {
                    Log.e(TAG, "Invalid start marker for depth or confidence data")
                }
            }
        }
    }

    /**
     * Utility function used to read input file into a byte array.
     */
    private fun loadInputBuffer(): ByteArray {
        val inputFile = File(location)
        return BufferedInputStream(inputFile.inputStream()).let { stream ->
            ByteArray(stream.available()).also {
                stream.read(it)
                stream.close()
            }
        }
    }

    /**
     * Utility function used to add an item to the viewpager and notify it, in the main thread.
     */
    private fun addItemToViewPager(view: ViewPager2, item: Bitmap) = view.post {
        bitmapList.add(item)
        view.adapter?.notifyItemChanged(bitmapList.size - 1)
    }

    /**
     * Utility function used to decode a [Bitmap] from a byte array
     */
    private fun decodeBitmap(buffer: ByteArray, start: Int, length: Int): Bitmap {

        // Load bitmap from given buffer
        val bitmap = BitmapFactory.decodeByteArray(buffer, start, length, bitmapOptions)

        // Transform bitmap orientation using provided metadata
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, bitmapTransformation, true,
        )
    }

    companion object {
        private val TAG = AdvancedImageViewer::class.java.simpleName

        /**
         * Argument keys
         */
        const val ARG_KEY_IS_DEPTH = "depth"
        const val ARG_KEY_ORIENTATION = "orientation"
        const val ARG_KEY_LOCATION = "location"

        /** Maximum size of [Bitmap] decoded */
        private const val DOWN_SAMPLE_SIZE: Int = 1024  // 1MP

        /** These are the magic numbers used to separate the different JPG data chunks */
        private val JPEG_DELIMITER_BYTES = arrayOf(-1, -39)

        /**
         * Utility function used to find the markers indicating separation between JPEG data chunks
         */
        private fun findNextJpegEndMarker(jpegBuffer: ByteArray, start: Int): Int {

            // Sanitize input arguments
            assert(start >= 0) { "Invalid start marker: $start" }
            assert(jpegBuffer.size > start) {
                "Buffer size (${jpegBuffer.size}) smaller than start marker ($start)"
            }

            // Perform a linear search until the delimiter is found
            for (i in start until jpegBuffer.size - 1) {
                if (jpegBuffer[i].toInt() == JPEG_DELIMITER_BYTES[0] &&
                    jpegBuffer[i + 1].toInt() == JPEG_DELIMITER_BYTES[1]
                ) {
                    return i + 2
                }
            }

            // If we reach this, it means that no marker was found
            throw RuntimeException("Separator marker not found in buffer (${jpegBuffer.size})")
        }
    }
}
