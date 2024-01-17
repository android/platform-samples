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

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.platform.ui.predictiveback.databinding.FragmentNavigationComponentDefaultAnimationsBinding

class PBNavigationComponentDefaultAnimations : Fragment() {

    private var _binding: FragmentNavigationComponentDefaultAnimationsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNavigationComponentDefaultAnimationsBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Handle insets so fragment is edge-to-edge
        // See nav_graph.xml for the associated predictive back animation
        binding.header.setOnApplyWindowInsetsListener { header, windowInsets ->
            val topBarInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            } else {
                windowInsets.systemWindowInsetTop
            }
            header.updatePadding(top = topBarInset)
            windowInsets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}