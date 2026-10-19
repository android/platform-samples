/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.platform.ui.appwidgets.remote_compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the Pet Buddy character (body with mesh noise texture, eyes, and optional evolved mouth)
 * in [StoreActivity] and [BuddyConfigActivity].
 */
@Composable
fun PetAvatar(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    contentDescription: String? = state.nickname,
) {
    val context = LocalContext.current
    val bitmap = remember(
        state.buddyName,
        state.eyes,
        state.mouth,
        state.buddyColor,
        state.isGrownUp,
    ) {
        PetBitmapRenderer.renderPetBitmap(context, state).asImageBitmap()
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
