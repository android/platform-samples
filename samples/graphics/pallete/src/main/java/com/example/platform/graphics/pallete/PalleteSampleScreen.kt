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

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
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
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {g
        PaletteViewer(imageId = R.drawable.img_carles_rabada_unsplash)
        PaletteViewer(imageId = R.drawable.img_adam_birkett_unsplash)
    }
}

@Composable
fun PaletteViewer(
    @DrawableRes imageId: Int,
) {
    var profileState: Palette? by remember { mutableStateOf(null) }

    BitmapFactory.decodeResource(
        LocalContext.current.resources,
        imageId,
    ).let { bitmap ->
        Palette.from(bitmap).generate { palette ->
            profileState = palette
        }
    }

    Column {
        profileState?.let { palette ->
            Text(
                text = "This text and background color is from vibrant swatch.",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(palette.vibrantSwatch?.titleTextColor ?: 0),
                ),
                modifier = Modifier
                    .clickable {
                    }
                    .fillMaxWidth()
                    .background(
                        color = Color(palette.vibrantSwatch?.rgb ?: 0),
                    ),
            )
            Row {
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
                Column {
                    palette.lightVibrantSwatch?.let { swatch ->
                        Text(
                            text = "Light Vibrant",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(swatch.titleTextColor),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(swatch.rgb),
                                )
                                .padding(5.dp),
                        )
                    }

                    palette.vibrantSwatch?.let { swatch ->
                        Text(
                            text = "Vibrant",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(swatch.titleTextColor),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(swatch.rgb),
                                )
                                .padding(5.dp),
                        )
                    }

                    palette.darkVibrantSwatch?.let { swatch ->
                        Text(
                            text = "Dark Vibrant",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(swatch.titleTextColor),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(swatch.rgb),
                                )
                                .padding(5.dp),
                        )
                    }

                    palette.lightMutedSwatch?.let { swatch ->
                        Text(
                            text = "Light Muted",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(swatch.titleTextColor),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(swatch.rgb),
                                )
                                .padding(5.dp),
                        )
                    }

                    palette.mutedSwatch?.let { swatch ->
                        Text(
                            text = "Muted",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(swatch.titleTextColor),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(swatch.rgb),
                                )
                                .padding(5.dp),
                        )
                    }

                    palette.darkMutedSwatch?.let { swatch ->
                        Text(
                            text = "Dark Muted",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(swatch.titleTextColor),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(swatch.rgb),
                                )
                                .padding(5.dp),
                        )
                    }
                }
            }
        }
    }
}
