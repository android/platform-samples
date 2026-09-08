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

package com.example.platform.ui.haptics.lavabeats

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun LavaBeatsGraphics(
    pulseTime: Float,
    beatEffectTimingParams: BeatEffectTimingParams,
    pulse: Boolean = false,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    BoxWithConstraints {
        val constraints = this
        val density = LocalDensity.current
        val width = with(density) { constraints.maxWidth.toPx() }
        val height = with(density) { constraints.maxHeight.toPx() }
        var startTimeMillis by remember { mutableLongStateOf(-1L) }
        var time by remember { mutableFloatStateOf(0f) }

        val lifecycleOwner = LocalLifecycleOwner.current
        val isInDarkMode = isSystemInDarkTheme()
        val surfaceColor = MaterialTheme.colorScheme.background

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                startTimeMillis = -1L
                while (true) {
                    withInfiniteAnimationFrameMillis { frameTime ->
                        if (startTimeMillis == -1L) {
                            startTimeMillis = frameTime
                        }
                        time = (frameTime - startTimeMillis) / 1000f
                    }
                }
            }
        }
        val shader = remember { LavaBeatsShader() }

        Box(
            modifier =
                Modifier.drawWithCache {
                        if (isInDarkMode) {
                            shader.enableDarkMode()
                        } else {
                            shader.enableLightMode()
                        }
                        shader.setBackground(surfaceColor)
                        shader.enablePulsing(pulse)
                        shader.setResolution(width, height)
                        shader.setTime(time)
                        shader.setPulseTime(pulseTime)
                        shader.setBeatEffectTimingParameters(beatEffectTimingParams)
                        val shaderBrush = ShaderBrush(shader)
                        onDrawBehind { drawRect(shaderBrush) }
                    }
                    .fillMaxSize()
        )
    }
}
