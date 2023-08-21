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
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrUsingFrescoBinding
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.launch

@Sample(
    name = "Displaying UltraHDR (Fresco)",
    description = "This sample demonstrates using the Fresco's BaseBitmapDataSubscriber class to " +
            "detect the presence of a gainmap & enable HDR mode when displaying an image using" +
            " this library.",
    documentation = "https://frescolib.org/",
    tags = ["UltraHDR"],
)
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRUsingFresco : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrUsingFrescoBinding? = null
    private val binding get() = _binding!!

    /**
     * Using [Fresco]'s [BaseBitmapDataSubscriber] class so we can access the given [Bitmap] to
     * check for the, presence of a gainmap, indicating that we should enable the HDR color mode.
     */
    private val subscriber = object : BaseBitmapDataSubscriber() {
        override fun onNewResultImpl(bitmap: Bitmap?) {
            binding.imageContainer.setImageBitmap(bitmap)
            lifecycleScope.launch {
                // Set color mode of the activity to the correct color mode.
                requireActivity().window.colorMode = when (bitmap?.hasGainmap()) {
                    true -> ActivityInfo.COLOR_MODE_HDR
                    else -> ActivityInfo.COLOR_MODE_DEFAULT
                }
            }
        }

        override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
            dataSource.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Fresco Library
        if (!Fresco.hasBeenInitialized()) Fresco.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DisplayingUltrahdrUsingFrescoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.colorModeControls.setWindow(requireActivity().window)
        loadImageWithFresco(SDR_IMAGE_URL)

        // Disable color mode controls to demonstrate glide enabling hdr mode when a gainmap is
        // is detected.
        binding.colorModeControls.binding.ultrahdrColorModeSdr.isEnabled = false
        binding.colorModeControls.binding.ultrahdrColorModeHdr.isEnabled = false

        binding.optionSdrImage.setOnClickListener { loadImageWithFresco(SDR_IMAGE_URL) }
        binding.optionUltrahdrImage.setOnClickListener { loadImageWithFresco(ULTRAHDR_IMAGE) }
    }

    /**
     * Load an image using [Fresco].
     */
    private fun loadImageWithFresco(path: String) {
        val imageRequest = ImageRequestBuilder
            .newBuilderWithSource(Uri.parse(path))
            .build()

        // Use Fresco's ImagePipeline to fetch and decode image into Bitmap subscriber.
        Fresco.getImagePipeline()
            .fetchDecodedImage(imageRequest, this)
            .apply { subscribe(subscriber, CallerThreadExecutor.getInstance()) }
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