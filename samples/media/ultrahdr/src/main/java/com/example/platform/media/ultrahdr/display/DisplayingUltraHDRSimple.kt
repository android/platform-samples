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
package com.example.platform.media.ultrahdr.display

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.platform.media.ultrahdr.databinding.DisplayingUltrahdrSimpleBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Displaying UltraHDR",
    description = "This sample demonstrates displaying an UltraHDR image",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["ultrahdr", "graphics"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRSimple : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrSimpleBinding? = null
    private val binding get() = _binding!!

    /**
     * List for UltraHDR Images
     */
    private val ultraHDRImages = mapOf(
        0 to ULTRA_HDR_IMAGE_LAMPS,
        1 to ULTRA_HDR_IMAGE_CANYON,
        2 to ULTRA_HDR_IMAGE_TRAIN_STATION,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DisplayingUltrahdrSimpleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.displayingUltrahdrColorModeControls.setWindow(requireActivity().window)

        binding.displayingUltrahdrSamplesSelectionGroup.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            val index = group.indexOfChild(selected)
            updateDisplayedImage(index)
        }
        binding.displayingUltrahdrLamps.isChecked = true
    }

    /**
     * Updated the currently displayed UltraHDR image.
     */
    private fun updateDisplayedImage(index: Int) {
        ultraHDRImages[index]?.let {
            binding.displayingUltrahdrImageContainer.setImageResource(0)
            val stream = context?.assets?.open(it)
            val bitmap = BitmapFactory.decodeStream(stream)
            binding.displayingUltrahdrImageContainer.setImageBitmap(bitmap)
        }
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_LAMPS = "gainmaps/lamps.jpg"
        private const val ULTRA_HDR_IMAGE_CANYON = "gainmaps/grand_canyon.jpg"
        private const val ULTRA_HDR_IMAGE_TRAIN_STATION = "gainmaps/train_station_night.jpg"
    }
}