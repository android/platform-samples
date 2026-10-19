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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the layered Pet Buddy character (body, eyes, and optional evolved mouth)
 * in [StoreActivity] and [BuddyConfigActivity].
 */
@Composable
fun PetAvatar(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    contentDescription: String? = state.nickname,
) {
    // Tint the pet body only when the user selects a custom theme color.
    val bodyColorFilter = if (state.buddyColor != BuddyPreferences.DEFAULT_BUDDY_COLOR) {
        ColorFilter.tint(Color(state.buddyColor))
    } else {
        null
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                id = BuddyPreferences.getBodyDrawableResForBuddy(state.buddyName),
            ),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            colorFilter = bodyColorFilter,
        )
        Image(
            painter = painterResource(id = BuddyPreferences.getEyesDrawableRes(state.eyes)),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(0.65f),
        )
        // Draw the mouth below the eyes only when the pet is in its evolved form.
        if (state.isGrownUp) {
            Image(
                painter = painterResource(id = BuddyPreferences.getMouthDrawableRes(state.mouth)),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = size * 0.045f),
            )
        }
    }
}
