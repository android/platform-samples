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
import com.example.platform.ui.draganddrop.databinding.FragmentDndMultiwindowBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Drag and Drop in MultiWindow mode",
    description = "Drag and drop to another app visible in multiwindow mode",
    documentation = "https://developer.android.com/develop/ui/views/touch-and-input/drag-drop/multi-window",
)
class DragAndDropMultiWindow : Fragment(R.layout.fragment_dnd_multiwindow) {
    lateinit var binding: FragmentDndMultiwindowBinding

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDndMultiwindowBinding.bind(view)
        ConstraintSet().clone(binding.root)
        binding.tvGreeting.text = resources.getString(R.string.multiwindow_greeting)
        binding.ivSource.tag = resources.getString(R.string.multi_source_image_url)
        Glide.with(this).asBitmap().load(resources.getString(R.string.multi_source_image_url))
            .into(binding.ivSource)
        Glide.with(this).asBitmap().load(resources.getString(R.string.multi_target_image_url))
            .into(binding.ivTarget)
        setupDrag(binding.ivSource)
        setupDrop(binding.ivTarget)

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupDrag(draggableView: ImageView) {
        DragStartHelper(draggableView) { view: View, _: DragStartHelper ->
            val item = ClipData.Item(view.tag as? CharSequence)
            val dragData = ClipData(
                view.tag as? CharSequence,
                arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                item,
            )
            // Flag is required so that data from clipdata can be read by the drop target.
            // view can directly specify the flags in this helper method.
            val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            view.startDragAndDrop(
                dragData,
                View.DragShadowBuilder(view),
                null,
                flags,
            )
        }.attach()
    }

   private fun setupDrop(targetView: ImageView) {
       /**
        * DropHelper manages permission to read data across app, provided DRAG permission has been
        * granted by drag source. No additional code is required as transient permission
        * are granted and released
        * Consider performing processing of [ClipData] in background if it is long-running
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