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

package com.example.platform.ui.haptics.rocket

import android.content.res.Resources
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.platform.ui.haptics.R
import com.example.platform.ui.haptics.components.Screen
import com.example.platform.ui.haptics.modifiers.noRippleClickable

private val ROCKET_HEIGHT_DP = 80.dp
private val ROCKET_FLAME_HEIGHT_DP = 17.dp
private val ROCKET_WIDTH_DP = 40.dp

private val FLOOR_HEIGHT_DP = 80.dp

private const val ANIMATION_LENGTH_MS = 3000



@Composable
fun RocketRoute(viewModel: RocketViewModel) {
    RocketExampleScreen(
        viewModel.messageToUser,
        viewModel.vibrator,
    )
}

@Composable
fun RocketExampleScreen(messageToUser: String, vibrator: Vibrator) {
    val context = LocalContext.current
    // get local density from composable
    val density = LocalDensity.current

    //initial height set at 0.dp, everything in Dp until inside DrawRocket
    var componentHeight by remember { mutableStateOf(0.dp) }
    var componentWidth by remember { mutableStateOf(0.dp) }

    var rocketYStartPosition by remember { mutableIntStateOf(0) }
    var inFlight by remember { mutableStateOf(false) }
    var rocketPositionY by remember { mutableFloatStateOf(0f) }
    val animation = remember { Animatable(0f) }

    LaunchedEffect(inFlight) {
        if (inFlight) {
            // kick off haptic
            if (RocketViewModel.isSupportedDevice(context)) {
                playEnvelopeVibration(
                    vibrator,
                    totalDurationMs = ANIMATION_LENGTH_MS.toLong(),
                )
            }

            // animate launch rocket
            animation.animateTo(
                1.2f,
                animationSpec = tween(
                    durationMillis = ANIMATION_LENGTH_MS,
                    // Applies an easing curve with a slow start and rapid acceleration towards the end.
                    easing = CubicBezierEasing(1f, 0f, 0.75f, 1f),
                ),
            ) {
                rocketPositionY = (componentHeight.value * value).dp.value
            }
            animation.snapTo(0f)
            rocketPositionY = 0f;
            inFlight = false;
        }
    }


    Screen(pageTitle = stringResource(R.string.rocket), messageToUser = messageToUser) {
        Surface(
            modifier = Modifier
                .noRippleClickable {
                    if (!inFlight) {
                        inFlight = true
                    }
                }
                // Calculate dimensions of container for Rocket
                .onGloballyPositioned {
                    componentHeight = with(density) { it.size.height.toDp() - FLOOR_HEIGHT_DP }
                    componentWidth = with(density) { it.size.width.toDp() }
                    rocketYStartPosition = componentHeight.value.toInt()
                },
        ) {
            if (!inFlight) DrawText(stringResource(R.string.rocket_tap_to_launch))

            DrawRocket(componentHeight, componentWidth, rocketPositionY.dp, inFlight)
            DrawFloor(MaterialTheme.colorScheme.primaryContainer, FLOOR_HEIGHT_DP)
        }
    }
}


@Composable
private fun DrawFloor(color: Color, thickness: Dp) {
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalDivider(
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            thickness = thickness,
        )
    }
}

@Composable
private fun DrawText(text: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = text,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(vertical = 100.dp),
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun DrawRocket(
    containerHeightDp: Dp,
    containerWidthDp: Dp,
    rocketFromFloorDp: Dp,
    launched: Boolean,
) {
    val rocketX = containerWidthDp / 2 - ROCKET_WIDTH_DP / 2
    val rocketY = containerHeightDp - rocketFromFloorDp - ROCKET_HEIGHT_DP + ROCKET_FLAME_HEIGHT_DP
    val resId = if (launched) {
        R.drawable.rocket_with_flame
    } else {
        R.drawable.rocket_without_flame
    }

    Image(
        modifier = Modifier
            .width(ROCKET_WIDTH_DP)
            .height(ROCKET_HEIGHT_DP)
            .offset(rocketX, rocketY),
        painter = painterResource(id = resId),
        contentDescription = "Rocket without flame",
    )
}


@RequiresApi(36)
private fun playEnvelopeVibration(
    vibrator: Vibrator,
    totalDurationMs: Long = 3000L,
    targetOutputAccelerationGs: Float = 0.1f,
    riseBias: Float = 0.7f,
) {
    require(riseBias in 0f..1f) { "Rise bias must be between 0 and 1." }

    if (!vibrator.areEnvelopeEffectsSupported()) {
        return
    }

    val frequencyProfile = vibrator.frequencyProfile ?: return
    val resonantFrequency = vibrator.resonantFrequency
    if (resonantFrequency.isNaN()) {
        return
    }

    val frequencyRange = frequencyProfile.getFrequencyRange(targetOutputAccelerationGs) ?: return
    if (frequencyRange.lower >= resonantFrequency) {
        return
    }

    val rampUpDuration = (riseBias * (totalDurationMs)).toLong()
    val rampDownDuration = totalDurationMs - rampUpDuration

    vibrator.vibrate(
        VibrationEffect.WaveformEnvelopeBuilder()
            .addControlPoint(0.1f, frequencyRange.lower, 20)
            .addControlPoint(0.1f, resonantFrequency, rampUpDuration)
            .addControlPoint(0.1f, frequencyRange.lower, rampDownDuration)
            .addControlPoint(0.0f, frequencyRange.lower, 20)
            .build(),
    )
}

