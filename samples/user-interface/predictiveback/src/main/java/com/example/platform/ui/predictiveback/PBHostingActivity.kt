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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.platform.ui.predictiveback.databinding.HostingActivityBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Predictive Back Sample",
    description = "Shows Predictive Back animations.",
    documentation = "https://developer.android.com/about/versions/14/features/predictive-back",
    tags = ["predictive"]
)
class PBHostingActivity : AppCompatActivity() {

    private lateinit var binding: HostingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = HostingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}