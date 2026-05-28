/*
 * Copyright 2026 The Android Open Source Project
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
 * See the License for the details;
 * and limitations under the License.
 */

package com.example.platform.ui.appwidgets.glance.layout.text

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeImageGridDataRepository
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeImageGridDataRepository.Companion.getImageGridDataRepo
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridItemData
import com.example.platform.ui.appwidgets.glance.layout.text.layout.FullBleedImageLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glance widget showcasing the Full Bleed Snapping Gallery layout, powered by the existing
 * memory-optimized [FakeImageGridDataRepository].
 */
class FullBleedImageAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = getImageGridDataRepo(id)

        val initialData = withContext(Dispatchers.Default) {
            repo.load(context)
        }

        provideContent {
            val data by repo.data().collectAsState(initial = initialData)

            GlanceTheme {
                key(LocalSize.current) {
                    WidgetContent(
                        data = data
                    )
                }
            }
        }
    }

    @Composable
    fun WidgetContent(data: List<ImageGridItemData>?) {
        FullBleedImageLayout(
            data = data
        )
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val repo = getImageGridDataRepo(AppWidgetId(0))

        val initialData = withContext(Dispatchers.Default) {
            repo.load(context)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    data = initialData
                )
            }
        }
    }
}

/**
 * Receiver for the Full Bleed Snapping Gallery widget.
 */
class FullBleedImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullBleedImageAppWidget()

    @SuppressLint("RestrictedApi")
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            FakeImageGridDataRepository.cleanUp(AppWidgetId(appWidgetId = it))
        }
        super.onDeleted(context, appWidgetIds)
    }
}
