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
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrUsingGlideBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Displaying UltraHDR (Glide)",
    description = "This sample demonstrates using the Glide image loading library to detect the" +
            " presence of a gainmap to enable HDR mode when displaying an image using this library.",
    documentation = "https://github.com/bumptech/glide",
    tags = ["UltraHDR"],
)
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRUsingGlide : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrUsingGlideBinding? = null
    private val binding get() = _binding!!

    /**
     * Using [Glide]s [CustomTarget] class, we can access the given [Bitmap] to check for the
     * presence of a gainmap, indicating that we should enable the HDR color mode.
     *
     * The same could be done with [CustomViewTarget] as well.
     */
    private val target: CustomTarget<Bitmap> = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            binding.imageContainer.setImageBitmap(resource)

            // Set color mode of the activity to the correct color mode.
            requireActivity().window.colorMode = when (resource.hasGainmap()) {
                true -> ActivityInfo.COLOR_MODE_HDR
                else -> ActivityInfo.COLOR_MODE_DEFAULT
            }
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            // clear resources if need be.
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DisplayingUltrahdrUsingGlideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.colorModeControls.setWindow(requireActivity().window)

        // Disable color mode controls to demonstrate glide enabling hdr mode when a gainmap is
        // is detected.
        binding.colorModeControls.binding.ultrahdrColorModeSdr.isEnabled = false
        binding.colorModeControls.binding.ultrahdrColorModeHdr.isEnabled = false

        binding.optionUltrahdrImage.setOnClickListener { loadImageWithGlide(ULTRA_HDR_IMAGE) }
        binding.optionSdrImage.setOnClickListener { loadImageWithGlide(NON_ULTRAHDR_IMAGE) }
        binding.optionSdrImage.performClick()
    }

    /**
     * Load an image using [Glide].
     */
    private fun loadImageWithGlide(path: String) =
        Glide.with(this)
            .asBitmap()
            .load(Uri.parse(path))
            .into(target)

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val NON_ULTRAHDR_IMAGE = "file:///android_asset/sdr/night_highrise.jpg"
        private const val ULTRA_HDR_IMAGE = "file:///android_asset/gainmaps/night_highrise.jpg"
    }
}