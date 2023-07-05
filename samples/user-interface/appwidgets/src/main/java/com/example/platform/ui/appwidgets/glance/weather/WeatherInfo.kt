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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes


sealed interface WeatherInfo {
    object Loading : WeatherInfo

    data class Available(
        val placeName: String,
        val currentData: WeatherData,
        val hourlyForecast: List<WeatherData>,
        val dailyForecast: List<WeatherData>,
    ) : WeatherInfo

    data class Unavailable(val message: String) : WeatherInfo
}

data class WeatherData(
    @DrawableRes val icon: Int,
    @StringRes val status: Int,
    val temp: Int,
    val maxTemp: Int,
    val minTemp: Int,
    val day: String,
    val hour: String,
)