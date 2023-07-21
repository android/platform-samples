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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrUsingGlideBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Sample(
    name = "Displaying UltraHDR Using Glide",
    description = "This sample demonstrates displaying an UltraHDR image using the Glide image" +
            " loading library",
    documentation = "https://github.com/bumptech/glide",
    tags = ["UltraHDR"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRUsingGlide : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrUsingGlideBinding? = null
    private val binding get() = _binding!!

    /**
     * List for UltraHDR Images
     */
    private val ultraHDRImages = mapOf(
        0 to ULTRA_HDR_IMAGE_LAMPS,
        1 to ULTRA_HDR_IMAGE_CANYON,
        2 to ULTRA_HDR_IMAGE_TRAIN_STATION,
    )

    private val imageViewTarget: Target<Bitmap>


    // Delete deprecated maybeEnableHdrOrWideGamut call after b/275438519 is
    // launched.
    private val photoViewTarget: Target<Bitmap> =
        object : CustomViewTarget<ImageView?, Bitmap?>(this) {
            override fun onResourceReady(
                resource: Bitmap, @Nullable transition: Transition<in Bitmap?>?,
            ) {
                onStaticDrawableReady(BitmapDrawable(resources, resource))
                if (colorSpaceFlags.get().useEnumForColorState()) {
                    if (displayModel != null && VERSION.SDK_INT >= VERSION_CODES.O) {
                        displayModel.setColorInfo(ColorInfo.Companion.fromBitmap(resource))
                    }
                } else {
                    // Let the screen color mode mixin know about the image being displayed so it can decide
                    // whether or not to turn on wide gamut mode.
                    Binder.get(context, ScreenColorModeMixin::class.java)
                        .maybeEnableHdrOrWideGamut(resource)
                }
            }

            override fun onLoadFailed(drawable: Drawable?) {}

            override fun onResourceCleared(@Nullable placeholder: Drawable?) {}
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

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.colorModeControls.setWindow(requireActivity().window)
        binding.imageOptionsSelectionGroup.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            val index = group.indexOfChild(selected)
            updateDisplayedImage(index)
        }
        binding.optionLamps.isChecked = true
    }

    /**
     * Updated the currently displayed UltraHDR image.
     */
    private fun updateDisplayedImage(index: Int) = lifecycleScope.launch(Dispatchers.IO) {
        ultraHDRImages[index]?.let {
            val stream = context?.assets?.open(it)
            val bitmap = BitmapFactory.decodeStream(stream)
            binding.imageContainer.setImageBitmap(bitmap)
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
        private const val ULTRA_HDR_IMAGE_LAMPS = "gainmaps/lamps.jpg"
        private const val ULTRA_HDR_IMAGE_CANYON = "gainmaps/grand_canyon.jpg"
        private const val ULTRA_HDR_IMAGE_TRAIN_STATION = "gainmaps/train_station_night.jpg"
    }
}