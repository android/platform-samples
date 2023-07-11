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

package com.example.platform.graphics.ultrahdr.display

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Gainmap
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.graphics.ultrahdr.R
import com.example.platform.graphics.ultrahdr.databinding.VisualizingAnUltrahdrGainmapBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Sample(
    name = "Visualizing an UltraHDR Gainmap",
    description = "This sample demonstrates visualizing the underlying gainmap of an UltraHDR " +
            "image, which reveals which parts of the image are enhanced by the gainmap.",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

class VisualizingAnUltraHDRGainmap : Fragment() {
    private enum class Type(val value: Int) {
        JPEG(0),
        GAINMAP(1),
        COMBINED(2);

        companion object {
            fun fromInt(value: Int) = Type.values().first { it.value == value }
        }
    }

    /**
     *  Android ViewBinding.
     */
    private var _binding: VisualizingAnUltrahdrGainmapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = VisualizingAnUltrahdrGainmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.colorModeControls.setWindow(requireActivity().window)
        binding.modeSelectionGroup.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            val index = group.indexOfChild(selected)
            updateDisplayedImage(Type.fromInt(index))
        }

        binding.modeJpeg.isChecked = true
    }

    /**
     * Updated the currently displayed image. This allows users to visualize the SDR image alone,
     * the gainmap as well as the result of the both of them displayed together.
     */
    private fun updateDisplayedImage(type: Type) = lifecycleScope.launch(Dispatchers.Main) {
        // Decode ultra hdr image into bitmap data.
        val imageBitmap = getBitmapFromFile()

        // Update radio group selection options based on whether or not the image has gainmap
        // contents or not.
        if (!imageBitmap.hasGainmap()) {
            binding.modeGainmap.text =
                resources.getText(R.string.visualizing_ultrahdr_gainmap_mode_title_no_gainmap)
            binding.modeGainmap.isEnabled = false

            binding.modeUltrahdr.text =
                resources.getText(R.string.visualizing_ultrahdr_gainmap_mode_title_no_ultrahdr)
            binding.modeUltrahdr.isEnabled = false
        }

        // Based on the type, setting the appropriate bitmap.
        binding.imageContainer.setImageBitmap(
            when (type) {
                // Intentionally drop the gainmap in order to display 8-bit jpeg sdr image. This
                // demonstrates that even if the activity is in HDR mode, SDR images will
                // remain unchanged.
                Type.JPEG -> imageBitmap.apply { gainmap = null }

                // Create a visual version of the gainmap where you will be able to see exactly
                // which parts of the UltraHDR image are being enhanced by the gainmap.
                Type.GAINMAP -> visualizeGainmap(imageBitmap)

                // Do nothing.
                Type.COMBINED -> imageBitmap
            },
        )
    }

    /**
     * Creates a monochrome representation of the [Gainmap] of an UltraHDR image and returns it as
     * a [Bitmap].
     */
    private suspend fun visualizeGainmap(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
        bitmap.gainmap?.let {
            val contents = it.gainmapContents
            if (contents.config != Bitmap.Config.ALPHA_8) return@withContext contents

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
            return@withContext visual
        }
        return@withContext bitmap
    }

    /**
     * Utility function to retrieve the bitmap representation of the file based on the location.
     */
    private suspend fun getBitmapFromFile(): Bitmap = withContext(Dispatchers.IO) {
        val location = arguments?.getString(ARG_KEY_LOCATION) ?: ""
        BitmapFactory.decodeStream(
            when (location.isNotBlank()) {
                true -> File(location).inputStream()
                false -> context?.assets?.open(ULTRA_HDR_IMAGE_FOUNTAIN)
            },
        )
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Default UltraHDR image path
         */
        private const val ULTRA_HDR_IMAGE_FOUNTAIN = "gainmaps/office.jpg"

        /**
         * Location argument for if this fragment is pass with a bundle that contains the location
         * of an UltraHDR image.
         */
        const val ARG_KEY_LOCATION = "location"
    }
}