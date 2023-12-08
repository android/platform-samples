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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.platform.graphics.ultrahdr.databinding.CompressUltrahdrBinding
import com.google.android.catalog.framework.annotations.Sample
import java.io.ByteArrayOutputStream


@RequiresApi(34)
@Sample(
    name = "Compressing UltraHDR Images",
    description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR"],
)

class CompressingUltraHDRImages : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: CompressUltrahdrBinding? = null
    private val binding get() = _binding!!
    var orignalImageSize: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CompressUltrahdrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.colorModeControls.setWindow(requireActivity().window)

        // Setup editing options and on listeners
        initializedUltraHDRImageButtonListeners()

        //  Set original ultra hdr image
        binding.optionOrignalUltrahdr.isChecked = true
        binding.optionOrignalUltrahdr.callOnClick()
    }

    private fun initializedUltraHDRImageButtonListeners() {

        //Select the edit operation : crop, rotate, scale
        binding.optionCompressedImage.setOnClickListener {
            compressAndDisplayUltraHDRImage()
        }

        //Select the edit operation : crop, rotate, scale
        binding.optionOrignalUltrahdr.setOnClickListener {
            displayOriginalUltraHDRImage()
        }
    }

    /**
     * Updated the currently displayed UltraHDR ORIGINAL image.
     */
    private fun displayOriginalUltraHDRImage() {

        val stream = context?.assets?.open(ULTRA_HDR_IMAGE)
        val bitmap = BitmapFactory.decodeStream(stream)
        binding.imageContainer.setImageBitmap(bitmap)

        // display original size of image
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val outputStreamSize = outputStream.toByteArray().size.toLong()

        // convert from bytes to MB for displaying text
        orignalImageSize = (outputStreamSize / (1024 * 1024))

        " Displayed Original Image size :  $orignalImageSize  MB".also {
            binding.ultrahdrImageSizeText.text = it
        }
    }

    /**
     * Updated the currently displayed UltraHDR ORIGINAL image.
     */
    private fun compressAndDisplayUltraHDRImage() {

        //open original image
        val stream = context?.assets?.open(ULTRA_HDR_IMAGE)
        val bitmap = BitmapFactory.decodeStream(stream)

        //Compress ultra-hdr image
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            ULTRA_HDR_IMAGE_COMPRESSION_QUALITY,
            outputStream,
        )


        // Display size of compressed image. For demo purposes only
        val imageInByte: ByteArray = outputStream.toByteArray()
        //convert fom bytes to megabytes
        val compressedImageSizeInMegabytes: Long = imageInByte.size.toLong() / (1024 * 1024)

        // Convert compressed stream into a bitmap
        val compressedByteArray: ByteArray = imageInByte
        val compressedBitmap =
            BitmapFactory
                .decodeByteArray(compressedByteArray, 0, compressedByteArray.size)


        //Load the compressed bitmap
        binding.imageContainer.setImageBitmap(compressedBitmap)

        //Set compressed image details as text for informational purposes
        ("Compressed Image Quality : ${ULTRA_HDR_IMAGE_COMPRESSION_QUALITY}% \n Displayed Compressed Image Size : ${compressedImageSizeInMegabytes}  MB ").also {
            binding.ultrahdrImageSizeText.text = it
        }
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE = "ultrahdr/ultrahdr_cityscape.jpg"

        //Image compression quality from 0-100. Refer: https://developer.android.com/reference/android/graphics/Bitmap.CompressFormat
        private const val ULTRA_HDR_IMAGE_COMPRESSION_QUALITY = 40
    }
}



