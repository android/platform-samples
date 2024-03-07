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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.platform.ui.draganddrop.databinding.GridItemBinding

class RichContentReceiverAdapter(private val items: List<GridItem>) :
    RecyclerView.Adapter<RichContentReceiverAdapter.DroppedItem>() {
    inner class DroppedItem(val binding: GridItemBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DroppedItem {
        val binding = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DroppedItem(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: DroppedItem, position: Int) {
        with(holder) {
            binding.droppedText.text = position.toString()

            when (items[position].mimeType) {
                "image/*" -> {
                    Glide.with(itemView.context).asBitmap().load(items[position].value)
                        .into(binding.ivDroppedImage)
                    binding.ivDroppedImage.visibility = VISIBLE
                }

                "text/*" -> {
                    binding.droppedText.text = items[position].value
                    binding.droppedText.visibility = VISIBLE
                }
            }
        }
    }
}

data class GridItem(val mimeType: String, val value: String)