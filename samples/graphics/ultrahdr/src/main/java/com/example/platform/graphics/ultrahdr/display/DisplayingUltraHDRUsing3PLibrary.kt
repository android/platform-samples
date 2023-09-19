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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrUsing3pLibraryBinding
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Sample(
    name = "Displaying UltraHDR (3P Libraries)",
    description = "This sample demonstrates using the various popular image loading library to" +
            " detect the presence of a gainmap to enable HDR mode when displaying an UltraHDR image",
    documentation = "https://github.com/bumptech/glide",
    tags = ["UltraHDR"],
)
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRUsing3PLibrary : Fragment() {
    private enum class Library(val value: Int) {
        GLIDE(0),
        FRESCO(1),
        COIL(2);

        companion object {
            fun fromInt(value: Int) = Library.values().first { it.value == value }
        }
    }

    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrUsing3pLibraryBinding? = null
    private val binding get() = _binding!!

    /**
     * Current 3p library selected.
     */
    private lateinit var currentLibrary: Library

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
        _binding = DisplayingUltrahdrUsing3pLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.colorModeControls.setWindow(requireActivity().window)

        // Disable color mode controls to demonstrate 3P libraries enabling hdr mode when a
        // gainmap is is detected.
        binding.colorModeControls.binding.ultrahdrColorModeSdr.isEnabled = false
        binding.colorModeControls.binding.ultrahdrColorModeSdr.text = "SDR (Automatic)"
        binding.colorModeControls.binding.ultrahdrColorModeHdr.isEnabled = false
        binding.colorModeControls.binding.ultrahdrColorModeHdr.text = "HDR (Automatic)"

        binding.librarySelectionGroup.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            val index = group.indexOfChild(selected)
            currentLibrary = Library.fromInt(index)
            loadWithSelectedLibrary(SDR_IMAGE_URL)
        }

        binding.optionSdrImage.setOnClickListener { loadWithSelectedLibrary(SDR_IMAGE_URL) }
        binding.optionUltrahdrImage.setOnClickListener { loadWithSelectedLibrary(ULTRAHDR_IMAGE) }

        // Initially load using Glide.
        binding.modeGlide.isChecked = true
    }

    /**
     * Load the image with the currently selected library.
     */
    private fun loadWithSelectedLibrary(url: String) {
        // Initially null out image bitmap
        binding.imageContainer.setImageBitmap(null)

        when (currentLibrary) {
            Library.GLIDE -> loadImageWithGlide(url)
            Library.FRESCO -> loadImageWithFresco(url)
            Library.COIL -> loadImageWithCoil(url)
        }

        // Disable corresponding button.
        when (url) {
            SDR_IMAGE_URL -> {
                binding.optionSdrImage.isEnabled = false
                binding.optionUltrahdrImage.isEnabled = true
            }

            ULTRAHDR_IMAGE -> {
                binding.optionSdrImage.isEnabled = true
                binding.optionUltrahdrImage.isEnabled = false
            }
        }
    }

    /**
     * Load an image using [Glide].
     */
    private fun loadImageWithGlide(path: String) {
        Glide.with(this)
            .asBitmap()
            .load(Uri.parse(path))
            .into(target)
    }

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

    /**
     * Load an image using [ImageLoader] & [ImageRequest] from Coil.
     */
    private fun loadImageWithCoil(url: String) {
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