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

import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.platform.ui.text.databinding.ConversionSuggestionsFragmentBinding
import com.google.android.catalog.framework.annotations.Sample

@RequiresApi(33)
@Sample(
    name = "Conversion suggestions",
    description = "Demonstrates how to implement the incremental search feature for non-alphabet languages with the Conversion Suggestions API.",
    documentation = "https://developer.android.com/about/versions/13/features#text-conversion",
    tags = ["text"]
)
class ConversionSuggestions : Fragment(R.layout.conversion_suggestions_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ConversionSuggestionsFragmentBinding.bind(view)

        // The ConversionEditText can return multiple search queries as user types.
        // In this sample, we just show the search queries.
        binding.edit.doOnSearchQueries { searchQueries ->
            binding.searchQueries.text = searchQueries.joinToString("\n")
        }
    }
}
