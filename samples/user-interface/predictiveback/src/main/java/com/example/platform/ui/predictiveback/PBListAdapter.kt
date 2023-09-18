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

package com.example.platform.ui.predictiveback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.platform.ui.predictiveback.databinding.ListItemAnimationBinding

class PBListAdapter(private val animations: List<PBAnimation>) : RecyclerView.Adapter<PBHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PBHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemAnimationBinding.inflate(inflater, parent, false)
        return PBHolder(binding)
    }

    override fun getItemCount() = animations.size

    override fun onBindViewHolder(holder: PBHolder, position: Int) {
        val animation = animations[position]
        holder.bind(animation)
    }
}