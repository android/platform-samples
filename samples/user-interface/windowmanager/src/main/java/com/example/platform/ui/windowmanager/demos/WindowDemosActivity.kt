/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.windowmanager.demos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.platform.ui.windowmanager.DisplayFeaturesActivity
import com.example.platform.ui.windowmanager.R
import com.example.platform.ui.windowmanager.SplitLayoutActivity
import com.example.platform.ui.windowmanager.WindowMetricsActivity
import com.example.platform.ui.windowmanager.embedding.SplitActivityList
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "WindowManager",
    description = "Demonstrates how to use the Jetpack WindowManager library.",
    documentation = "https://developer.android.com/jetpack/androidx/releases/window",
)
class WindowDemosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_demos)
        val demoItems = listOf(
            DemoItem(
                buttonTitle = getString(R.string.activity_embedding),
                description = getString(R.string.activity_embedding_description),
                clazz = SplitActivityList::class.java
            ),
            DemoItem(
                buttonTitle = getString(R.string.display_features),
                description = getString(R.string.show_all_display_features_config_change_description),
                clazz = DisplayFeaturesActivity::class.java
            ),
            DemoItem(
                buttonTitle = getString(R.string.window_metrics),
                description = getString(R.string.window_metrics_description),
                clazz = WindowMetricsActivity::class.java
            ),
            DemoItem(
                buttonTitle = getString(R.string.split_layout),
                description = getString(R.string.split_layout_demo_description),
                clazz = SplitLayoutActivity::class.java
            ),
        )
        val recyclerView = findViewById<RecyclerView>(R.id.demo_recycler_view)

        recyclerView.adapter = DemoAdapter(demoItems)
    }
}