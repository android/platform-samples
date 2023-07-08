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
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Gainmap
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.camera.databinding.AdvancedImageViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AdvancedImageViewer : Fragment() {
    private enum class Type(val value: Int) {
        JPEG(0),
        JPEG_R(1);

        companion object {
            fun fromInt(value: Int) = Type.values().first { it.value == value }
        }
    }

    /**
     * Location of the image file to load.
     */
    private val location: String by lazy {
        requireArguments().getString(ARG_KEY_LOCATION, "")
    }

    /**
     * [Bitmap] of the loaded image for.
     */

    /**
     *  Android ViewBinding.
     */
    private var _binding: AdvancedImageViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AdvancedImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            val bitmap = getBitmapFromFile()
            binding.imageContainer.setImageBitmap(bitmap)
        }
    }

    /**
     * Utility function to retrieve the bitmap representation of the file based on the [location].
     */
    private suspend fun getBitmapFromFile(): Bitmap = withContext(Dispatchers.IO) {
        val inputStream = File(location).inputStream()
        BitmapFactory.decodeStream(inputStream)
    }

    /**
     * Creates a monochrome representation of the [Gainmap] of an UltraHDR image and returns it as
     * a [Bitmap].
     */
    @RequiresApi(34)
    private fun visualizeGainmap(gainmap: Gainmap): Bitmap {
        val contents = gainmap.gainmapContents
        if (contents.config != Bitmap.Config.ALPHA_8) return contents

        val visual = Bitmap.createBitmap(
            contents.width, contents.height,
            Bitmap.Config.ARGB_8888,
        )

        val canvas = Canvas(visual)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(
            floatArrayOf(
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 255f,
            ),
        )
        canvas.drawBitmap(contents, 0f, 0f, paint)
        canvas.setBitmap(null)
        return visual
    }

    companion object {
        private val TAG = AdvancedImageViewer::class.java.simpleName

        /**
         * Argument keys
         */
        const val ARG_KEY_LOCATION = "location"
    }
}
