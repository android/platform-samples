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
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import com.example.platform.ui.text.databinding.LinkifyFragmentBinding
import com.google.android.catalog.framework.annotations.Sample
import java.util.regex.Pattern

@Sample(
    name = "Linkify",
    description = "Linkify is useful for creating links in TextViews.",
    documentation = "https://developer.android.com/reference/kotlin/androidx/core/text/util/LinkifyCompat",
    tags = ["text"]
)
class Linkify: Fragment(R.layout.linkify_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = LinkifyFragmentBinding.bind(view)
        // Linkify supports EMAIL_ADDRESSES, PHONE_NUMBERS, and WEB_URLS.
        LinkifyCompat.addLinks(binding.webUrls, android.text.util.Linkify.WEB_URLS)

        // Linkify a custom pattern using regex. This pattern represents a Twitter account.
        val pattern = Pattern.compile("@[a-zA-Z0-9_]{1,15}")
        LinkifyCompat.addLinks(
            binding.custom,
            pattern,
            "https://twitter.com/",
            null
        ) { _, url ->
            // Remove "@" at the beginning of the match.
            url.toString().substring(startIndex = 1)
        }
    }
}
