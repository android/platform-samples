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

package com.example.platform.ui.appwidgets.glance.weather

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Handle system events for AppWidgets with the provided GlanceAppWidget instance.
 *
 * Use this class to handle widget lifecycle specific events like onEnable/Disable.
 */
@RequiresApi(Build.VERSION_CODES.O)
class WeatherGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WeatherGlanceWidget()

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        CoroutineScope(Dispatchers.IO).launch {
            WeatherRepo.updateWeatherInfo()
        }
    }

}