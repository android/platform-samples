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
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.ContentInfoCompat
import androidx.core.view.DragStartHelper
import androidx.draganddrop.DropHelper
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.platform.ui.draganddrop.databinding.FragmentDragAndDropWithHelperBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Drag and Drop - Helper",
    description = "Drag and Drop using the DragHelper and DropHelper from DragAndDropHelper library",
    documentation = "https://developer.android.com/develop/ui/views/touch-and-input/drag-drop#drophelper",
)
@RequiresApi(Build.VERSION_CODES.N)
class DragAndDropWithHelper : Fragment(R.layout.fragment_drag_and_drop_with_helper) {

    private lateinit var binding: FragmentDragAndDropWithHelperBinding

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDragAndDropWithHelperBinding.bind(view)
        ConstraintSet().clone(binding.root)

        binding.tvGreeting.text = getString(R.string.helper_greeting)
        /*
        data for drag
        for simplicity we have considered text as a data type to drag
        For rich content like Image to be used as drag data type please check the sample for
        Drag and Drop - rich content
         */
        binding.ivSource.tag = getString(R.string.helper_source_image_url)
        Glide.with(this).asBitmap().load(getString(R.string.helper_source_image_url))
            .into(binding.ivSource)
        Glide.with(this).asBitmap().load(getString(R.string.helper_target_image_url))
            .into(binding.ivTarget)
        setupDrag(binding.ivSource)
        setupDrop(binding.ivTarget)
        // resetting the to state before drag
        binding.btnReset.setOnClickListener {
            Glide.with(this).asBitmap().load(getString(R.string.helper_target_image_url))
                .into(binding.ivTarget)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupDrag(draggableView: ImageView) {/*
        DragStartHelper is a utility class for implementing drag and drop support
        DragStartHelper provide the ease of implementation for dragging the view.
         */
        DragStartHelper(draggableView) { view: View, _: DragStartHelper ->
            val item = ClipData.Item(view.tag as? CharSequence)
            val dragData = ClipData(
                view.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item,
            )
            view.startDragAndDrop(
                dragData,
                View.DragShadowBuilder(view),
                null,
                0,
            )
        }.attach()
    }

    private fun setupDrop(targetView: ImageView) {/*
        Similar to drag method, drop in normal way be handled with implementing the listener for
        Drop event,
         */
        DropHelper.configureView(
            requireActivity(),
            targetView,
            arrayOf("text/*"),
        ) { _, payload: ContentInfoCompat ->
            val item = payload.clip.getItemAt(0)
            val dragData = item.text
            Glide.with(this).load(dragData).centerCrop().into(targetView)
            val (_, remaining) = payload.partition { it == item }
            remaining
        }
    }
}