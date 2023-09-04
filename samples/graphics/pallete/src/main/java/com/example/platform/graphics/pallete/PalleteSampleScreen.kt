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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Sample(
    name = "Palette API",
    description = "This sample demonstrates using an Palette API.",
    documentation = "https://developer.android.com/develop/ui/views/graphics/palette-colors#kts",
    tags = ["Graphics"],
)
@Composable
fun PaletteSampleScreen() {
    var profileState: Palette? by remember { mutableStateOf(null) }

    ContextCompat.getDrawable(
        LocalContext.current,
        R.drawable.img_carles_rabada_unsplash,
    )?.let { drawable ->
        Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
            Palette.from(bitmap).generate { palette ->
                profileState = palette
            }
        }
    }

    Column {
        profileState?.let { pallete ->
            Text(
                text = "This text and background color is from vibrant swatch.",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(pallete.vibrantSwatch?.titleTextColor ?: 0),
                ),
                modifier = Modifier.background(
                    color = Color(pallete.vibrantSwatch?.bodyTextColor ?: 0),
                ),
            )
        }
        Row {
            Image(
                painter = painterResource(id = R.drawable.img_carles_rabada_unsplash),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            Column {

            }
        }
    }
}
