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

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.platform.ui.appwidgets.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.O)
object WeatherRepo {

    private const val TIMEOUT = 10L

    private var _currentWeather = MutableStateFlow<WeatherInfo>(WeatherInfo.Loading)
    val currentWeather: StateFlow<WeatherInfo> get() = _currentWeather

    private var lastRun: Instant = Instant.EPOCH
    private val mutex = Mutex()

    /**
     * Request the WeatherInfo of a given location
     */
    suspend fun updateWeatherInfo(delay: Long = Random.nextInt(1, 3) * 1000L) {
        //Because multiple widgets may request a weather update at once,
        //we will put a simple timeout check here.
        mutex.withLock(lastRun) {
            if (lastRun.plusSeconds(TIMEOUT).isAfter(Instant.now())) {
                //30 seconds have not elapsed since last run.
                return
            } else {
                lastRun = Instant.now()
            }
        }

        // Simulate network loading
        _currentWeather.value = WeatherInfo.Loading

        if (delay > 0) {
            delay(delay)
        }

        _currentWeather.value =
            WeatherInfo.Available(
                placeName = "Tokyo",
                currentData = getRandomWeatherData(Instant.now()),
                hourlyForecast = (1..4).map {
                    getRandomWeatherData(Instant.now().plusSeconds(it * 3600L))
                },
                dailyForecast = (1..4).map {
                    getRandomWeatherData(Instant.now().plusSeconds(it * 86400L))
                },
            )
    }

    /**
     * Fake the weather data
     */
    private fun getRandomWeatherData(instant: Instant): WeatherData {
        val hourFormatter = DateTimeFormatterBuilder().appendPattern("h a").toFormatter()
        val dateTime = instant.atZone(ZoneId.systemDefault())

        return WeatherData(
            icon = R.drawable.ic_partly_cloudy,
            status = R.string.mostly_cloudy,
            temp = Random.nextInt(5, 35),
            maxTemp = Random.nextInt(5, 35),
            minTemp = Random.nextInt(5, 35),
            day = dateTime.dayOfWeek.name,
            hour = dateTime.format(hourFormatter),
        )
    }
}
