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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.platform.pallete.R
import com.google.android.catalog.framework.annotations.Sample


@Sample(
    name = "Testing Palette API",
    description = "This sample demonstrates using an Palette API.",
    documentation = "https://developer.android.com/develop/ui/views/graphics/palette-colors#kts",
    tags = ["Graphics"],
)
@Composable
fun PaletteSampleScreen() {
    Row {
        Image(
            painter = painterResource(id = R.drawable.img_carles_rabada_unsplash),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight(0.5f)
        )
    }
}
