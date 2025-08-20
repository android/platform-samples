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

package com.example.platform.ui.windowmanager

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.computeWindowSizeClass

import com.example.platform.ui.windowmanager.infolog.InfoLogAdapter

class WindowMetricsActivity : AppCompatActivity() {

    private val adapter = InfoLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_metrics)
        findViewById<RecyclerView>(R.id.recycler_view).adapter = adapter
        adapter.append("onCreate", "triggered")

        updateMetrics()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateMetrics()
    }

    private fun updateMetrics() {
        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val width = windowMetrics.bounds.width()
        val height = windowMetrics.bounds.height()
        val density = resources.displayMetrics.density
        val windowSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(width/density, height/density)

        adapter.append("WindowMetrics update", "width: $width, height: $height")
        adapter.append("WindowSize Class", "Height: " + getHeightSizeClass(windowSizeClass) + "\nWidth: " + getWidthSizeClass(windowSizeClass))
        adapter.notifyDataSetChanged()
    }

    private fun getWidthSizeClass(sizeClass: WindowSizeClass): String {
        return when {
            sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
                "EXPANDED"
            }
            sizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
                "MEDIUM"
            }
            else -> {
                "COMPACT"
            }
        }
    }

    private fun getHeightSizeClass(sizeClass: WindowSizeClass): String {
        return when {
            sizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND) -> {
                "EXPANDED"
            }
            sizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND) -> {
                "MEDIUM"
            }
            else -> {
                "COMPACT"
            }
        }
    }
}
