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
package com.example.platform.graphics.ultrahdr.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
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
import com.example.platform.graphics.ultrahdr.databinding.EditingUltrahdrBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


@Sample(
    name = "Editing UltraHDR",
    description = "This sample demonstrates editing an UltraHDR image and the resulting gainmap as well. Spatial edit operations like crop, rotate, scale are supported",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class EditingUltraHDR : Fragment() {


    /**
     *  Android ViewBinding.
     */
    private var _binding: EditingUltrahdrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = EditingUltrahdrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The ColorModeControls Class contain the necessary function to change the activities
        // ColorMode to HDR, which allows and UltraHDRs images gain map to be used to enhance the
        // image.
        binding.colorModeControls.setWindow(requireActivity().window)

        // Setup editing options and on listeners
        initializedImagesOrGainmap()
    }

    private fun initializedImagesOrGainmap() {
        displayOriginalUltraHDRImage()

        //Set default editing for type image and operation rotate
        var editingType: Int = ULTRA_HDR_DISPLAY_EDITED_IMAGE
        var editOperation = ULTRA_HDR_IMAGE_ROTATE

        //Select which type to edit : Image or Gainmap (for visualization purposes only)
        binding.imageOptionVisualization.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            editingType = group.indexOfChild(selected)
        }

        //Select the edit operation : crop, rotate, scale
        binding.imageOptionsEditingGroup.setOnCheckedChangeListener { group, i ->
            val selected = group.findViewById<RadioButton>(i)
            editOperation = group.indexOfChild(selected)
            editSelectedUltraHDRImage(editOperation, editingType)
        }

        //Initialize refresh button
        binding.refreshUltrahdrEditedImage.setOnClickListener {
            editSelectedUltraHDRImage(editOperation,editingType)
        }

    }
    /**
     * Updated the currently displayed UltraHDR ORIGINAL image.
     */
    private fun displayOriginalUltraHDRImage() {

        val stream = context?.assets?.open(ULTRA_HDR_IMAGE_TRAIN_STATION)
        val bitmap = BitmapFactory.decodeStream(stream)
        binding.imageContainer.setImageBitmap(bitmap)
    }

    /**
     * Edit currently displayed UltraHDR image
     */
    private fun editSelectedUltraHDRImage(editOperation: Int, editingType: Int) =
        lifecycleScope.launch(Dispatchers.IO) {
            //Performing edit operations on a background thread

            val stream = context?.assets?.open(ULTRA_HDR_IMAGE_TRAIN_STATION)
            val bitmap = BitmapFactory.decodeStream(stream)

            //first display original image, then perform any edit operations
            displayOriginalUltraHDRImage()

            //Perform Edit operation
            lateinit var editedBitmap: Bitmap

            when (editOperation) {
                ULTRA_HDR_IMAGE_CROP -> editedBitmap = bitmap.crop()
                ULTRA_HDR_IMAGE_ROTATE -> editedBitmap = bitmap.rotate()
                ULTRA_HDR_IMAGE_SCALE -> editedBitmap = bitmap.scale()
            }

            //Display edited image or gainmap, run on main thread to update the UI
            withContext(Dispatchers.Main) {
                when (editingType) {
                    ULTRA_HDR_DISPLAY_EDITED_IMAGE -> binding.imageContainer.setImageBitmap(editedBitmap)
                    ULTRA_HDR_DISPLAY_EDITED_GAINMAP -> binding.imageContainer.setImageBitmap(
                        visualizeEditedGainmap(editedBitmap),
                    )
                }
                //Release memory
                bitmap.recycle()
          }
        }

    private fun Bitmap.rotate(): Bitmap {

        val matrix = Matrix().apply { postRotate(ULTRA_HDR_IMAGE_ROTATE_DEGREE) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.scale(): Bitmap {

        //For simplicity : Resize the image to X% of existing dimensions
        return Bitmap.createScaledBitmap(
            this, (this.width * ULTRA_HDR_IMAGE_SCALE_RATIO).roundToInt(),
            (this.height * ULTRA_HDR_IMAGE_SCALE_RATIO).roundToInt(), true,
        )
    }

    private fun Bitmap.crop(): Bitmap {

        //For simplicity : Crop to square shape
        val dimension: Int = this.width.coerceAtMost(this.height)
        return Bitmap.createBitmap(this, 0, 0, dimension, dimension)
    }

    private suspend fun visualizeEditedGainmap(bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.IO) {
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

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

    companion object {
        /**
         * Sample UltraHDR images paths
         */
        private const val ULTRA_HDR_IMAGE_TRAIN_STATION = "gainmaps/train_station_night.jpg"

        /**
         * Sample UltraHDR images operations
         */
        private const val ULTRA_HDR_IMAGE_CROP = 0
        private const val ULTRA_HDR_IMAGE_ROTATE = 1
        private const val ULTRA_HDR_IMAGE_SCALE = 2

        /**
         * Visualization types for editing
         */
        private const val ULTRA_HDR_DISPLAY_EDITED_IMAGE = 0
        private const val ULTRA_HDR_DISPLAY_EDITED_GAINMAP = 1

        private const val ULTRA_HDR_IMAGE_ROTATE_DEGREE = 180F
        private const val ULTRA_HDR_IMAGE_SCALE_RATIO = 0.2
    }

}

