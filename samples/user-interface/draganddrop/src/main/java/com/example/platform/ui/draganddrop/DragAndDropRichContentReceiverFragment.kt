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
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.ContentInfoCompat
import androidx.core.view.DragStartHelper
import androidx.draganddrop.DropHelper
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.platform.ui.draganddrop.databinding.FragmentDndRichcontentBinding
import com.google.android.catalog.framework.annotations.Sample
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


@Sample(
    name = "Drag and Drop using the RichContentReceiver",
    description = "Using RichContentReceiverInterface for implementing Drop for rich data types",
    documentation = "https://developer.android.com/develop/ui/views/receive-rich-content",
)
@RequiresApi(Build.VERSION_CODES.S)
class DragAndDropRichContentReceiverFragment : Fragment(R.layout.fragment_dnd_richcontent) {
    private val TAG = DragAndDropRichContentReceiverFragment::class.java.simpleName
    private lateinit var binding: FragmentDndRichcontentBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDndRichcontentBinding.bind(view)
        ConstraintSet().clone(binding.root)
        val items = mutableListOf<GridItem>()
//        binding.tvGreeting.text = getString(R.string.rich_content_greeting)
        initViews(items)
        initDrag()

        // setOnReceiveContentListener accepts data from various input sources
        // based on the type of view.
        // here Target View is made to handle Drag and Drop Events
        binding.rvDropTarget.setOnReceiveContentListener(
            arrayOf("image/*", "text/*", "video/*"),
        ) { _, payload ->
            val (uriContent, remaining) = ContentInfoCompat.partition(
                payload,
            ) { item: ClipData.Item -> item.uri != null }

            if (uriContent != null) {
                val item = uriContent.clip.getItemAt(0)
                item.uri?.let { uri ->
                    val size = 160.px
                    decodeSampledBitmapFromUri(
                        requireActivity().contentResolver,
                        uri,
                        size,
                        size,
                    )?.let { bitmap ->
                        items.add(GridItem("image/*", uri.toString()))
                        (binding.rvDropTarget.adapter as RichContentReceiverAdapter).notifyItemInserted(
                            items.size - 1,
                        )
                    }
                } ?: run {
                    Log.e(TAG, "Clip data is missing URI")
                }
            }
            if (remaining != null) {
                val item = remaining.clip.getItemAt(0)
                items.add(GridItem("text/*", item.text.toString()))
                (binding.rvDropTarget.adapter as RichContentReceiverAdapter).notifyItemInserted(
                    items.size - 1,
                )

            }
            null
        }
    }

    private fun initViews(items: MutableList<GridItem>) {
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.rvDropTarget.layoutManager = layoutManager
        val adapter = RichContentReceiverAdapter(items)
        binding.rvDropTarget.adapter = adapter

        //using DropHelper to define shadow for drop area
        DropHelper.configureView(
            requireActivity(),
            binding.rvDropTarget,
            arrayOf("image/*", "text/*", "video/*"),
            DropHelper.Options.Builder()
                .setHighlightColor(ContextCompat.getColor(requireContext(), R.color.purple_300))
                .setHighlightCornerRadiusPx(16.px).build(),
        ) { _, payload: ContentInfoCompat ->
            payload
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun initDrag() {
        Glide.with(this).asBitmap().load(getString(R.string.rich_source_image_url))
            .into(binding.ivSource1)
        addDragStartHelperForImageView(binding.ivSource1)
        Glide.with(this).asBitmap().load(getString(R.string.rich_target_image_url))
            .into(binding.ivSource)
        addDragStartHelperForImageView(binding.ivSource)

        binding.tvDrag.text = getString(R.string.rich_content_greeting)
        addDragStartHelperForTextView(binding.tvDrag)

    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun addDragStartHelperForTextView(draggbleView: View) {
        DragStartHelper(draggbleView) { view: View, _: DragStartHelper ->
            val dragData = ClipData.newPlainText("DraggedText", (view as TextView).text)
            val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            view.startDragAndDrop(
                dragData,
                View.DragShadowBuilder(view),
                null,
                flags,
            )
        }.attach()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun addDragStartHelperForImageView(draggableView: View) {
        DragStartHelper(draggableView) { view: View, _: DragStartHelper ->
            val filename = System.currentTimeMillis().toString()
            val imageFile = File(File(requireActivity().filesDir, "images"), filename + ".png")
            ByteArrayOutputStream().use { bos ->
                (view as ImageView).drawable.toBitmap().compress(Bitmap.CompressFormat.PNG, 0, bos)
                FileOutputStream(imageFile).use { fos ->
                    fos.write(bos.toByteArray())
                    fos.flush()
                }
            }
            val imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireActivity().packageName}.draganddrop.provider",
                imageFile,
            )
            val dragData = ClipData.newUri(
                requireActivity().contentResolver,
                "Image",
                imageUri,
            )
            val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            view.startDragAndDrop(
                dragData,
                View.DragShadowBuilder(view),
                null,
                flags,
            )
        }.attach()
    }
}
