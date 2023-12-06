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
package com.example.platform.graphics.ultrahdr.display

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Window
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider
import com.example.platform.graphics.ultrahdr.R
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.function.Consumer

@RequiresApi(34)
@Sample(
    name = "Displaying UltraHDR (Compose)",
    description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR", "Compose"],
)

@Composable
fun DisplayUltraHDRScreen() {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    var colorMode by remember { mutableStateOf<ColorMode>(ColorMode.Default) }
    val window = findWindow()
    val display = LocalView.current.display

    // Load asset and bitmap on background thread
    LaunchedEffect(Unit) {
        window?.let {
            colorMode = getColorMode(it, display)
        }

        // Same bitmap is used to load image in an image view and image (Compose)
        bitmap = withContext(Dispatchers.IO) {
            val ultraHdrImage = "gainmaps/night_highrise.jpg"
            context.assets.open(ultraHdrImage).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }
    }

    val hdrSdrRatioChangeListener = Consumer<Display> { display ->
        if (window == null) return@Consumer

        Log.d(TAG, "HDR/SDR Ratio Changed ${display.hdrSdrRatio}")

        colorMode = getColorMode(window, display)
    }

    DisposableEffect(window, display) {
        if (display.isHdrSdrRatioAvailable) {
            display.registerHdrSdrRatioChangedListener(
                { executable -> executable.run() },
                hdrSdrRatioChangeListener,
            )
        }
        // When the effect leaves the Composition, remove the observer
        onDispose {
            display.unregisterHdrSdrRatioChangedListener(hdrSdrRatioChangeListener)
        }
    }


    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val details = when (colorMode) {
            is ColorMode.Default -> stringResource(R.string.color_mode_sdr)
            is ColorMode.Unknown -> stringResource(R.string.color_mode_unknown)
            is ColorMode.Hdr -> stringResource(
                R.string.color_mode_hdr_with_ratio,
                (colorMode as ColorMode.Hdr).hdrSdrRatio,
            )
        }

        Text(stringResource(R.string.color_mode_details, details))

        // Add SDR/HDR Color mode controls
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(dimensionResource(R.dimen.ultrahdr_color_mode_current_mode_padding)),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT },
            ) {
                Text(stringResource(R.string.color_mode_sdr))
            }
            Spacer(Modifier.width(dimensionResource(R.dimen.ultrahdr_color_mode_current_mode_padding)))
            Button(
                modifier = Modifier.weight(1f),
                onClick = { window?.colorMode = ActivityInfo.COLOR_MODE_HDR },
            ) {
                Text(stringResource(R.string.color_mode_hdr))
            }
        }

        // Render UltraHDR in a Compose Image
        Text(text = "Image (Compose)")

        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.weight(1f),
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary,
        )

        // Render UltraHDR in View (ImageView)
        Text(text = "ImageView (Android View)")
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = {
                ImageView(it).apply {
                    setImageBitmap(bitmap)
                }
            },
            update = {
                it.setImageBitmap(bitmap)
            },
        )
    }
}

private const val TAG = "DisplayUltraHDRScreen"

private sealed interface ColorMode {
    object Default : ColorMode
    object Unknown : ColorMode

    @JvmInline
    value class Hdr(val hdrSdrRatio: Float) : ColorMode
}

@Composable
private fun findWindow(): Window? =
    (LocalView.current.parent as? DialogWindowProvider)?.window ?: LocalContext.current.findWindow()

private tailrec fun Context.findWindow(): Window? =
    when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.findWindow()
        else -> null
    }

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun getColorMode(window: Window, display: Display) = when (window.colorMode) {
    ActivityInfo.COLOR_MODE_DEFAULT -> ColorMode.Default
    ActivityInfo.COLOR_MODE_HDR -> ColorMode.Hdr(display.hdrSdrRatio)
    else -> ColorMode.Unknown
}