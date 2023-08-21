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

import android.content.pm.ActivityInfo
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrUsingCoilBinding
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrUsingGlideBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Sample(
    name = "Displaying UltraHDR (Coil)",
    description = "This sample demonstrates using the Glide image loading library to detect the" +
            " presence of a gainmap to enable HDR mode when displaying an image using this library.",
    documentation = "https://github.com/bumptech/glide",
    tags = ["UltraHDR"],
)
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRUsingCoil : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrUsingCoilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DisplayingUltrahdrUsingCoilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.colorModeControls.setWindow(requireActivity().window)
        loadImageIntoView(SDR_IMAGE_URL)

        // Disable color mode controls to demonstrate glide enabling hdr mode when a gainmap is
        // is detected.
        binding.colorModeControls.binding.ultrahdrColorModeSdr.isEnabled = false
        binding.colorModeControls.binding.ultrahdrColorModeHdr.isEnabled = false

        binding.optionSdrImage.setOnClickListener { loadImageIntoView(SDR_IMAGE_URL) }
        binding.optionUltrahdrImage.setOnClickListener { loadImageIntoView(ULTRAHDR_IMAGE) }
    }

    /**
     * Load an image using [ImageLoader] & [ImageRequest] from Coil.
     */
    private fun loadImageIntoView(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val loader = ImageLoader(requireContext())
            val request = ImageRequest.Builder(requireContext())
                .data(url)
                .crossfade(true)
                .crossfade(100)
                .build()

            val result = loader.execute(request)

            // convert bitmap from drawable.
            val bitmap = (result.drawable as BitmapDrawable).bitmap

            // Activate HDR mode if bitmap has gain map
            withContext(Dispatchers.Main) {
                binding.imageContainer.setImageBitmap(bitmap)
                requireActivity().window.colorMode = when (bitmap.hasGainmap()) {
                    true -> ActivityInfo.COLOR_MODE_HDR
                    else -> ActivityInfo.COLOR_MODE_DEFAULT
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val SDR_IMAGE_URL =
            "https://raw.githubusercontent.com/android/platform-samples/main/samples/graphics/" +
                    "ultrahdr/src/main/assets/sdr/night_highrise.jpg"
        private const val ULTRAHDR_IMAGE =
            "https://raw.githubusercontent.com/android/platform-samples/main/samples/graphics/" +
                    "ultrahdr/src/main/assets/gainmaps/night_highrise.jpg"
    }
}