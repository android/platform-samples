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

package com.example.platform.graphics.pallete

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.compose.ImagePainter
import com.example.platform.pallete.R
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.delay

@Sample(
    name = "Palette API",
    description = "This sample demonstrates using an Palette API.",
    documentation = "https://developer.android.com/develop/ui/views/graphics/palette-colors#kts",
    tags = ["Graphics"],
)
@Composable
fun PaletteSampleScreen() {
    var profileState: Palette? by remember { mutableStateOf(null) }

    BitmapFactory.decodeResource(
        LocalContext.current.resources,
        R.drawable.test_2,
    ).let { bitmap ->
        if (profileState == null) {
            Palette.from(bitmap).generate { palette ->
                profileState = palette
            }
        }
    }

    Column {
        profileState?.let { pallete ->
            Log.d("TAG", "PaletteSampleScreen: $pallete")
            Log.d("TAG", "PaletteSampleScreen: ${pallete.getLightVibrantColor(0)}")
            Log.d("TAG", "PaletteSampleScreen: ${pallete.getVibrantColor(0)}")
            Log.d("TAG", "PaletteSampleScreen: ${pallete.getDarkVibrantColor(0)}")
            Log.d("TAG", "PaletteSampleScreen: ${pallete.getLightMutedColor(0)}")
            Log.d("TAG", "PaletteSampleScreen: ${pallete.getMutedColor(0)}")
            Log.d("TAG", "PaletteSampleScreen: ${pallete.getDarkMutedColor(0)}")

            Text(
                text = "This text and background color is from vibrant swatch.",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(pallete.vibrantSwatch?.titleTextColor ?: 0),
                ),
                modifier = Modifier
                    .clickable {
                    }
                    .fillMaxWidth()
                    .background(
                        color = Color(pallete.vibrantSwatch?.rgb ?: 0),
                    ),
            )
        }
        Row {
            Image(
                painter = painterResource(id = R.drawable.test_2),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            Column {
                profileState?.let {
                    it.swatches.forEach { swatch ->
                        if (swatch.bodyTextColor != 0) {
                            Text(
                                text = it.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(swatch.titleTextColor),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(swatch?.rgb ?: 0),
                                    ),
                            )
                        }
                    }
                }

            }
        }
    }
}
