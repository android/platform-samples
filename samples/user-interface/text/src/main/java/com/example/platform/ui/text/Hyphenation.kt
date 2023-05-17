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

package com.example.platform.ui.text

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.platform.ui.text.databinding.HyphenationFragmentBinding
import com.example.platform.ui.text.utils.doOnItemSelected
import com.google.android.catalog.framework.annotations.Sample

@RequiresApi(Build.VERSION_CODES.M)
@Sample(
    name = "Hyphenation",
    description = "Demonstrates different options for the `android:hyphenationFrequency` attribute",
    documentation = "https://developer.android.com/reference/android/widget/TextView#attr_android:hyphenationFrequency",
    tags = ["text"]
)
class Hyphenation : Fragment(R.layout.hyphenation_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = HyphenationFragmentBinding.bind(view)

        // Set up the control.
        binding.hyphenationFrequency.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            android.R.id.text1,
            HyphenationFrequencyOption.values().map { it.label }
        )
        binding.hyphenationFrequency.doOnItemSelected { _, position, _ ->
            binding.paragraphs.hyphenationFrequency =
                HyphenationFrequencyOption.values()[position].value
        }
    }
}

@SuppressLint("InlinedApi")
private enum class HyphenationFrequencyOption(
    val value: Int,
    val label: String,
) {
    FullFast(Layout.HYPHENATION_FREQUENCY_FULL_FAST, "fullFast (API 33+)"),
    Full(Layout.HYPHENATION_FREQUENCY_FULL, "full"),
    None(Layout.HYPHENATION_FREQUENCY_NONE, "none"),
    NormalFast(Layout.HYPHENATION_FREQUENCY_NORMAL_FAST, "normalFast (API 33+)"),
    Normal(Layout.HYPHENATION_FREQUENCY_NORMAL, "normal"),
}
