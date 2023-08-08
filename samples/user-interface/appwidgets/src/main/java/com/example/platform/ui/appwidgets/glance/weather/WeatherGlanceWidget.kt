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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.glance.AppWidgetBox
import com.example.platform.ui.appwidgets.glance.AppWidgetColumn
import com.example.platform.ui.appwidgets.glance.appWidgetInnerCornerRadius
import com.example.platform.ui.appwidgets.glance.stringResource
import kotlinx.coroutines.launch
import java.util.Locale

class WeatherGlanceWidget : GlanceAppWidget() {

    companion object {
        private val thinMode = DpSize(120.dp, 120.dp)
        private val smallMode = DpSize(184.dp, 184.dp)
        private val mediumMode = DpSize(260.dp, 200.dp)
        private val largeMode = DpSize(260.dp, 280.dp)
    }

    // Define the supported sizes for this widget.
    // The system will decide which one fits better based on the available space
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(thinMode, smallMode, mediumMode, largeMode),
    )


    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun Content() {
        // Get the stored stated based on our custom state definition.
        val weatherInfo by WeatherRepo.currentWeather.collectAsState()
        // It will be one of the provided ones
        val size = LocalSize.current

        //Create the update weather on click lambda
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val updateWeather: () -> Unit = { scope.launch { refreshWeather(context) } }

        GlanceTheme {
            when (val currentWeather = weatherInfo) {
                WeatherInfo.Loading -> {
                    AppWidgetBox(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is WeatherInfo.Available -> {
                    // Based on the size render different UI
                    when (size) {
                        thinMode -> WeatherThin(currentWeather, updateWeather)
                        smallMode -> WeatherSmall(currentWeather, updateWeather)
                        mediumMode -> WeatherMedium(currentWeather, updateWeather)
                        largeMode -> WeatherLarge(currentWeather, updateWeather)
                    }
                }

                is WeatherInfo.Unavailable -> {
                    AppWidgetColumn(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Data not available")
                        Button("Refresh", updateWeather)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherThin(weatherInfo: WeatherInfo.Available, onClick: () -> Unit) {
    AppWidgetColumn(GlanceModifier.clickable(onClick)) {
        CurrentTemperature(
            weatherInfo,
            modifier = GlanceModifier.fillMaxSize(),
            Alignment.CenterHorizontally,
        )
    }
}

@Composable
fun WeatherSmall(weatherInfo: WeatherInfo.Available, onClick: () -> Unit) {
    AppWidgetColumn(GlanceModifier.clickable(onClick)) {
        Row(
            modifier = GlanceModifier.wrapContentHeight().fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            WeatherIcon(weatherInfo, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
            PlaceWeather(weatherInfo, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
        }
        CurrentTemperature(weatherInfo, modifier = GlanceModifier.fillMaxSize(), Alignment.Start)
    }
}

@Composable
fun WeatherMedium(weatherInfo: WeatherInfo.Available, onClick: () -> Unit) {
    AppWidgetColumn(GlanceModifier.clickable(onClick)) {
        Row(
            modifier = GlanceModifier.wrapContentHeight().fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            WeatherIcon(weatherInfo, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
            PlaceWeather(weatherInfo, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
        }
        Row(
            modifier = GlanceModifier.wrapContentHeight().fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            CurrentTemperature(
                weatherInfo,
                modifier = GlanceModifier.fillMaxHeight(),
                Alignment.Start,
            )
            HourlyForecast(weatherInfo, modifier = GlanceModifier.fillMaxSize())
        }
    }
}

@Composable
fun WeatherLarge(weatherInfo: WeatherInfo.Available, onClick: () -> Unit) {
    AppWidgetColumn(GlanceModifier.clickable(onClick)) {
        Row(
            modifier = GlanceModifier.wrapContentHeight().fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            WeatherIcon(weatherInfo, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
            PlaceWeather(weatherInfo, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
        }
        Row(
            modifier = GlanceModifier.wrapContentHeight().fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            CurrentTemperature(
                weatherInfo,
                modifier = GlanceModifier.wrapContentHeight(),
                Alignment.Start,
            )
            HourlyForecast(weatherInfo, modifier = GlanceModifier.fillMaxWidth())
        }
        Spacer(GlanceModifier.size(8.dp))
        DailyForecast(weatherInfo)
    }
}

@Composable
fun WeatherIcon(weatherInfo: WeatherInfo.Available, modifier: GlanceModifier = GlanceModifier) {
    // TODO missing tint
    Box(modifier = modifier, contentAlignment = Alignment.TopStart) {
        Image(
            provider = ImageProvider(weatherInfo.currentData.icon),
            contentDescription = stringResource(weatherInfo.currentData.status),
            modifier = GlanceModifier.size(48.dp),
        )
    }
}

@Composable
fun CurrentTemperature(
    weatherInfo: WeatherInfo.Available,
    modifier: GlanceModifier = GlanceModifier,
    horizontal: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = horizontal,
    ) {
        val defaultWeight = GlanceModifier.wrapContentSize()
        Text(
            text = "${weatherInfo.currentData.temp}°",
            style = TextStyle(
                fontSize = 48.sp,
            ),
            modifier = defaultWeight,
        )
        Row(modifier = defaultWeight) {
            Text(
                text = "${weatherInfo.currentData.minTemp}°",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.size(8.dp))
            Text(
                text = "${weatherInfo.currentData.maxTemp}º",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
fun PlaceWeather(
    weatherInfo: WeatherInfo.Available,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.End,
    ) {
        val defaultWeight = GlanceModifier.defaultWeight()
        Text(
            text = weatherInfo.placeName,
            style = TextStyle(
                fontSize = 18.sp,
                textAlign = TextAlign.End,
            ),
            modifier = defaultWeight,
        )
        Text(
            text = stringResource(weatherInfo.currentData.status),
            style = TextStyle(
                fontSize = 12.sp,
                textAlign = TextAlign.End,
            ),
            modifier = defaultWeight,
        )
    }
}

@Composable
fun HourlyForecast(
    weatherInfo: WeatherInfo.Available,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        weatherInfo.hourlyForecast.forEach {
            HourForecast(it)
        }
    }
}

@Composable
fun HourForecast(
    weatherData: WeatherData,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${weatherData.temp}º",
            style = TextStyle(
                fontSize = 14.sp,
            ),
        )
        Image(
            provider = ImageProvider(weatherData.icon),
            contentDescription = stringResource(weatherData.status),
            modifier = GlanceModifier.size(24.dp),
        )
        Text(
            text = weatherData.hour,
            style = TextStyle(
                fontSize = 14.sp,
            ),
        )
    }
}

@Composable
fun DailyForecast(
    weatherInfo: WeatherInfo.Available,
    modifier: GlanceModifier = GlanceModifier,
) {
    LazyColumn(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.surfaceVariant)
            .appWidgetInnerCornerRadius()
            .then(modifier),
    ) {
        items(weatherInfo.dailyForecast) { dayForecast ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dayForecast.toDayString(),
                    style = TextStyle(
                        fontSize = 14.sp,
                    ),
                )
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val textStyle = TextStyle(
                        fontSize = 14.sp,
                    )
                    Image(
                        provider = ImageProvider(dayForecast.icon),
                        contentDescription = stringResource(dayForecast.status),
                        modifier = GlanceModifier.size(24.dp).padding(4.dp),
                    )
                    Text(
                        text = "${dayForecast.minTemp}º",
                        style = textStyle,
                        modifier = GlanceModifier.padding(4.dp),
                    )
                    Text(
                        text = "${dayForecast.maxTemp}º",
                        style = textStyle,
                        modifier = GlanceModifier.padding(4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Force update the weather info after user click
 */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun refreshWeather(context: Context) {
    // Force the widget to refresh
    WeatherGlanceWidget().updateAll(context)
    WeatherRepo.updateWeatherInfo()
}

@Composable
private fun WeatherData.toDayString() = day.lowercase(Locale.getDefault()).replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

