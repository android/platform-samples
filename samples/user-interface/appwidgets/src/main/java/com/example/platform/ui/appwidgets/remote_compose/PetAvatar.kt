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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders the layered Pet Buddy character (tinted body, eyes, and optional evolved mouth)
 * shared across [StoreActivity] and [BuddyConfigActivity].
 */
@Composable
fun PetAvatar(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
    containerSize: Dp = 104.dp,
    characterSize: Dp = 88.dp,
) {
    PetAvatar(
        buddyName = state.buddyName,
        eyes = state.eyes,
        mouth = state.mouth,
        buddyColor = state.buddyColor,
        eyeMouthColor = state.eyeMouthColor,
        isGrownUp = state.isGrownUp,
        contentDescription = state.nickname,
        modifier = modifier,
        containerSize = containerSize,
        characterSize = characterSize,
    )
}

@Composable
fun PetAvatar(
    buddyName: String,
    eyes: String,
    mouth: String = BuddyPreferences.DEFAULT_MOUTH,
    buddyColor: Int = BuddyPreferences.DEFAULT_BUDDY_COLOR,
    eyeMouthColor: Int = BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR,
    isGrownUp: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    containerSize: Dp = 104.dp,
    characterSize: Dp = containerSize,
) {
    val bodyColorFilter = if (
        buddyColor != 0 && buddyColor != BuddyPreferences.DEFAULT_BUDDY_COLOR
    ) {
        ColorFilter.tint(Color(buddyColor), BlendMode.SrcIn)
    } else {
        null
    }
    val eyeMouthColorFilter = createEyeMouthColorFilter(eyeMouthColor)
    val bodyPainter = painterResource(
        id = BuddyPreferences.getBodyDrawableResForBuddy(buddyName),
    )

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .size(characterSize)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            val bodyIntrinsic = bodyPainter.intrinsicSize
            val bodyAspect = if (bodyIntrinsic.width > 0f && bodyIntrinsic.height > 0f) {
                bodyIntrinsic.width / bodyIntrinsic.height
            } else {
                1f
            }
            val boxWidth = maxWidth
            val boxHeight = maxHeight
            val boxAspect = if (boxHeight > 0.dp) boxWidth / boxHeight else 1f
            val renderedBodyWidth = if (bodyAspect >= boxAspect) boxWidth else boxHeight * bodyAspect
            val renderedBodyHeight = if (bodyAspect >= boxAspect) boxWidth / bodyAspect else boxHeight

            val eyesWidth = renderedBodyWidth * 0.65f
            val eyesOffsetY = renderedBodyHeight * (BuddyPreferences.EYES_CENTER_Y_FRACTION - 0.50f)
            val mouthWidth = renderedBodyWidth * 1.0f
            val mouthOffsetY = renderedBodyHeight * (BuddyPreferences.MOUTH_CENTER_Y_FRACTION - 0.50f)

            Image(
                painter = bodyPainter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                colorFilter = bodyColorFilter,
            )
            Image(
                painter = painterResource(id = BuddyPreferences.getEyesDrawableRes(eyes)),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .width(eyesWidth)
                    .offset(y = eyesOffsetY),
                colorFilter = eyeMouthColorFilter,
            )
            val mouthRes = BuddyPreferences.getMouthDrawableRes(
                mouth,
                isGrownUp = isGrownUp,
            )
            if (mouthRes != null) {
                Image(
                    painter = painterResource(id = mouthRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .width(mouthWidth)
                        .offset(y = mouthOffsetY),
                    colorFilter = eyeMouthColorFilter,
                )
            }
        }
    }
}

/**
 * Creates a white-preserving [ColorFilter] that tints black (`#000000`) pupils, outlines, and mouths
 * to [eyeMouthColorArgb] while preserving white (`#FFFFFF`) eye sclera areas as pure white.
 */
internal fun createEyeMouthColorFilter(eyeMouthColorArgb: Int): ColorFilter {
    val targetColor = if (eyeMouthColorArgb == 0) {
        BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR
    } else {
        eyeMouthColorArgb
    }
    val r = ((targetColor ushr 16) and 0xFF).toFloat()
    val g = ((targetColor ushr 8) and 0xFF).toFloat()
    val b = (targetColor and 0xFF).toFloat()
    val scaleR = 1f - (r / 255f)
    val scaleG = 1f - (g / 255f)
    val scaleB = 1f - (b / 255f)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                scaleR, 0f, 0f, 0f, r,
                0f, scaleG, 0f, 0f, g,
                0f, 0f, scaleB, 0f, b,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
