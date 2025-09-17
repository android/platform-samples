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

package com.example.platform.ui.haptics.spring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.delay

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.platform.ui.haptics.R
import com.example.platform.ui.haptics.components.Screen
import com.example.platform.ui.haptics.modifiers.noRippleClickable

private const val GRAVITY = 2f
private const val BOUNCE_DAMPING = 0.85f
private const val INITIAL_VELOCITY = 3.3f
private const val INITIAL_SHARPNESS = 0.2f
private const val INITIAL_INTENSITY = 0.1f
private const val INITIAL_MULTIPLIER = 0.95f
private const val MAX_BOTTOM_BOUNCE = 3
private const val FRAME_DELAY_MS = 16L
private val FLOOR_HEIGHT = 80.dp
private val SPRING_HEIGHT = 45.dp
private val SPRING_WIDTH = 35.dp

@Composable
fun SpringRoute(viewModel: SpringViewModel) {
    SpringExampleScreen(viewModel.messageToUser, viewModel.vibrator)
}

@Composable
fun SpringExampleScreen(messageToUser: String, vibrator: Vibrator) {
    val context = LocalContext.current
    //initial height set at 0.dp
    var componentHeight by remember { mutableStateOf(0.dp) }
    var componentWidth by remember { mutableStateOf(0.dp) }

    // get local density from composable
    val density = LocalDensity.current

    var springX by remember { mutableStateOf(SPRING_WIDTH) }
    var springY by remember { mutableStateOf(SPRING_HEIGHT) }

    var velocityX by remember { mutableFloatStateOf(INITIAL_VELOCITY) }
    var velocityY by remember { mutableFloatStateOf(INITIAL_VELOCITY) }
    var sharpness by remember { mutableFloatStateOf(INITIAL_SHARPNESS) }
    var intensity by remember { mutableFloatStateOf(INITIAL_INTENSITY) }
    var multiplier by remember { mutableFloatStateOf(INITIAL_MULTIPLIER) }

    var bottomBounceCount by remember { mutableIntStateOf(0) }
    var animationStartTime by remember { mutableLongStateOf(0L) }

    // State to trigger animation restart
    var animationTrigger by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    val resetAnimation = remember {
        {
            springX = SPRING_WIDTH
            springY = SPRING_HEIGHT
            velocityX = INITIAL_VELOCITY
            velocityY = INITIAL_VELOCITY
            sharpness = INITIAL_SHARPNESS
            intensity = INITIAL_INTENSITY
            multiplier = INITIAL_MULTIPLIER
            bottomBounceCount = 0
            animationStartTime = System.currentTimeMillis()
            isAnimating = false
            animationTrigger = !animationTrigger // Toggle the trigger
        }
    }



    LaunchedEffect(animationTrigger) {
        animationStartTime = System.currentTimeMillis()
        isAnimating = true

        while (isAnimating) {
            velocityY += GRAVITY
            springX += velocityX.dp
            springY += velocityY.dp

            // Handle bottom collision
            if (springY > componentHeight - FLOOR_HEIGHT - SPRING_HEIGHT / 2) {
                // Set the spring's Y position to the bottom bounce point, ensuring it remains above the floor.
                springY = componentHeight - FLOOR_HEIGHT - SPRING_HEIGHT / 2
                // Reverse the vertical velocity and apply damping to simulate a bounce.
                velocityY *= -BOUNCE_DAMPING
                bottomBounceCount++

                if (SpringViewModel.isSupportedOnDevice(context)) {
                    playEnvelopeVibration(vibrator, velocityY, intensity, sharpness)
                }

                // Decrease the intensity and sharpness of the vibration for subsequent bounces,
                // and reduce the multiplier to create a fading effect.
                intensity *= multiplier
                sharpness *= multiplier
                //Log.e("SPRING","$velocityY, $intensity, $sharpness, $multiplier")
                multiplier = (multiplier - 0.1f).coerceAtLeast(0f)
            }

            if (springX > componentWidth - SPRING_WIDTH / 2) {
                // Prevent the spring from moving beyond the right edge of the screen.
                springX = componentWidth - SPRING_WIDTH / 2
            }

            // Check for 3 bottom bounces and then slow down
            if (bottomBounceCount >= MAX_BOTTOM_BOUNCE && System.currentTimeMillis() - animationStartTime > 1000) {
                velocityX *= 0.9f
                velocityY *= 0.9f
            }

            delay(FRAME_DELAY_MS) // Control animation speed

            // Determine if the animation should continue based on the spring's position and velocity.
            isAnimating =
                (springY < componentHeight + SPRING_HEIGHT || springX < componentWidth + SPRING_WIDTH)
                        && (velocityX >= 0.1f || velocityY >= 0.1f)
        }
    }

    Screen(pageTitle = stringResource(R.string.spring), messageToUser = messageToUser) {
        Surface(
            modifier = Modifier
                //.border(2.dp, Color.Green)
                .onGloballyPositioned {
                    componentHeight = with(density) {
                        it.size.height.toDp()
                    }
                    componentWidth = with(density) {
                        it.size.width.toDp()
                    }
                }
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .noRippleClickable {
                        if (!isAnimating) {
                            resetAnimation()
                        }
                    }
                    .width(componentWidth)
                    .height(componentHeight)
                    .background(Color.Transparent),
            ) {
                if (!isAnimating) {
                    DrawText(stringResource(R.string.spring_tap_to_restart))
                }
                DrawSpring(MaterialTheme.colorScheme.primaryContainer, springX, springY)
                DrawFloor(MaterialTheme.colorScheme.primaryContainer)
            }
        }
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
private fun DrawFloor(color: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalDivider(
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            thickness = FLOOR_HEIGHT,
        )
    }
}


@RequiresApi(36)
private fun playEnvelopeVibration(
    vibrator: Vibrator,
    velocityY: Float,
    intensity: Float,
    sharpness: Float,
) {
    if (!vibrator.areEnvelopeEffectsSupported()) return

    require(sharpness in 0f..1f) { "Sharpness must be between 0 and 1." }
    require(intensity in 0f..1f) { "intensity must be between 0 and 1." }

    val minControlPointDurationMs = vibrator.envelopeEffectInfo.minControlPointDurationMillis ?: return

    // Calculate the fade-out duration of the vibration based on the vertical velocity.
    val fadeOutDuration = ((abs(velocityY) / GRAVITY) * FRAME_DELAY_MS).toLong()
    // Create a <<bo-ing>> envelope vibration effect that fades out.
    vibrator.vibrate(
        VibrationEffect.BasicEnvelopeBuilder()
            // Starting from zero sharpness here, will simulate a smoother <<bo-ing>> effect.
            .setInitialSharpness(0f)
            // Add a control point to reach the desired intensity and sharpness as quickly as possible
            .addControlPoint(intensity, sharpness, minControlPointDurationMs)
            // Add a control point to fade out the vibration intensity while maintaining sharpness.
            .addControlPoint(0f, sharpness, fadeOutDuration).build(),
    )
}

/**
 * I hope there's a better way to create a spring-like shape.
 */
@Composable
private fun DrawSpring(color: Color, springX: Dp, springY: Dp) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numberOfCoils = 5 // Adjust for desired coil density
        val coilResolution = 20 // Points per coil, adjust for smoothness

        // Calculate spring parameters
        val x1 = springX.toPx()
        val y1 = springY.toPx() - SPRING_HEIGHT.toPx() / 2 // Start at top of spring
        val x2 = springX.toPx()
        val y2 = springY.toPx() + SPRING_HEIGHT.toPx() / 2 // End at bottom of spring
        val width = SPRING_WIDTH.toPx()
        val x = x2 - x1
        val y = y2 - y1
        val dist = kotlin.math.sqrt(x * x + y * y)
        val nx = y / dist // Normal vector components (swapped for correct direction)
        val ny = -x / dist
        val coilSpacing = dist / numberOfCoils

        // Draw the spring
        val path = Path()
        path.moveTo(x1, y1)

        for (i in 0..numberOfCoils) {
            val coilStart = i * coilSpacing
            for (j in 0..coilResolution) {
                val angle = j * 2 * Math.PI / coilResolution
                val positionAlongSpring = coilStart + (coilSpacing * j / coilResolution)

                val xx = x1 + (x * positionAlongSpring / dist) +
                        kotlin.math.cos(angle) * nx * width / 2
                val yy = y1 + (y * positionAlongSpring / dist) +
                        kotlin.math.cos(angle) * ny * width / 2

                path.lineTo(xx.toFloat(), yy.toFloat())
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 5f, cap = StrokeCap.Round),
        )
    }
}
