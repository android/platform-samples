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

package com.example.platform.ui.draganddrop

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.platform.ui.draganddrop.databinding.FragmentDragAndDropWithViewsBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Drag and Drop using views",
    description = "Drag and Drop using the views",
    documentation = "https://developer.android.com/develop/ui/views/touch-and-input/drag-drop/view",
)
@RequiresApi(Build.VERSION_CODES.N)
class DragAndDropWithViews : Fragment(R.layout.fragment_drag_and_drop_with_views) {

    val TAG = "DragAndDropWithViews"
    private lateinit var binding: FragmentDragAndDropWithViewsBinding

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDragAndDropWithViewsBinding.bind(view)
        ConstraintSet().clone(binding.root)

        binding.tvGreeting.text = getString(R.string.view_greeting)
        binding.ivSource.tag = getString(R.string.views_source_image_url)
        Glide.with(this).asBitmap().load(getString(R.string.views_source_image_url))
            .into(binding.ivSource)
        Glide.with(this).asBitmap().load(getString(R.string.views_target_image_url))
            .into(binding.ivTarget)
        setupDrag(binding.ivSource)
        setupDrop(binding.ivTarget)
        // resetting the to state before drag
        binding.btnReset.setOnClickListener {
            Glide.with(this).asBitmap().load(getString(R.string.views_target_image_url))
                .into(binding.ivTarget)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupDrag(draggableView: ImageView) {
        draggableView.setOnLongClickListener { v ->
            val label = "Dragged Image Url"
            val clipItem = ClipData.Item(v.tag as? CharSequence)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val draggedData = ClipData(
                label, mimeTypes, clipItem
            )
            v.startDragAndDrop(
                draggedData,
                View.DragShadowBuilder(v),
                null,
               View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupDrop(dropTarget: ImageView) {
       dropTarget.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    Log.d(TAG, "ON DRAG STARTED")
                    if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        (v as? ImageView)?.alpha = 0.5F
                        v.invalidate()
                        true
                    } else {
                        false
                    }
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    Log.d(TAG, "ON DRAG ENTERED")
                    (v as? ImageView)?.alpha = 0.3F
                    v.invalidate()
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    Log.d(TAG, "On DRAG LOCATION")
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.d(TAG, "ON DRAG ENDED")
                    (v as? ImageView)?.alpha = 1.0F
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    Log.d(TAG, "ON DRAG EXISITED")
                    (v as? ImageView)?.alpha = 0.5F
                    v.invalidate()
                    true
                }

                DragEvent.ACTION_DROP -> {
                    Log.d(TAG, "On DROP")
                    val dropPermission = requireActivity().requestDragAndDropPermissions(event)
                    val item: ClipData.Item = event.clipData.getItemAt(0)
                    val dragData = item.text
                    Toast.makeText(requireContext(), "Dragged Data ${dragData}", Toast.LENGTH_SHORT).show()
                    Glide.with(this).load(item.text).into(v as ImageView)
                    (v as? ImageView)?.alpha = 1.0F
                    dropPermission.release()
                    true
                }

                else -> {
                    false
                }
            }
        }
    }
}