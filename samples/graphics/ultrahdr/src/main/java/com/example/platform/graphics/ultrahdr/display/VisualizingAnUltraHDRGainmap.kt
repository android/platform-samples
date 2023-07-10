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
import com.example.platform.graphics.ultrahdr.databinding.VisualizingAnUltrahdrGainmapBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Visualizing an UltraHDR Gainmap",
    description = "This sample demonstrates visualizing the underlying gainmap of an UltraHDR " +
            "image, which reveals which parts of the image are enhanced by the gainmap.",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["ultrahdr"],
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
    private fun updateDisplayedImage(type: Type) {
        // Clear previous bitmap source.
        val imageview = binding.imageContainer
        imageview.setImageResource(0)

        // Decode ultra hdr image into bitmap data.
        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_FOUNTAIN)
        val imageBitmap = BitmapFactory.decodeStream(stream)

        // Based on the type, setting the appropriate bitmap.
        imageview.setImageBitmap(
            when (type) {
                // Intentionally drop the gainmap in order to display 8-bit jpeg sdr image. This
                // demonstrates that even if the activity is in HDR mode, SDR images will
                // remain unchanged.
                Type.JPEG -> imageBitmap.apply { gainmap = null }

                // Create a visual version of the gainmap where you will be able to see exactly
                // which parts of the UltraHDR image are being enhanced by the gainmap.
                Type.GAINMAP -> visualizeGainmap(imageBitmap.gainmap!!)

                // Do nothing.
                Type.COMBINED -> imageBitmap
            },
        )
    }

    /**
     * Creates a monochrome representation of the [Gainmap] of an UltraHDR image and returns it as
     * a [Bitmap].
     */
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

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Sample UltraHDR image path
         */
        private const val ULTRA_HDR_IMAGE_FOUNTAIN = "gainmaps/fountain_night.jpg"
    }
}