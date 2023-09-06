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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.platform.graphics.ultrahdr.R
import com.example.platform.graphics.ultrahdr.common.ColorModeControls
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrBinding
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.viewinterop.AndroidView as AndroidView

@RequiresApi(34)
@Sample(
    name = "Displaying UltraHDR (Compose)",
    description = "This sample demonstrates displaying an UltraHDR image in a Compose View and an Android View",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR", "Compose"],
)

@Composable
fun RenderComposeView() {

    val tempBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565)
    var bitmap by remember { mutableStateOf(tempBitmap) }
    val context = LocalContext.current

    // Load asset and bitmap on background thread
    LaunchedEffect(Unit) {

        // Same bitmap is used to load image in an image view and image (Compose)
        bitmap = withContext(Dispatchers.IO) {
            val ultraHdrImage = "gainmaps/night_highrise.jpg"
            val stream = context.assets.open(ultraHdrImage)
            BitmapFactory.decodeStream(stream)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // Add SDR/HDR Color mode controls
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = {
                ColorModeControls(it)
                    .apply {
                        setWindow((it as ComponentActivity).window)
                    }
            },
        )

        // Render UltraHDR in a Compose Image
        Text(text = "Image (Compose)")
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.weight(1f),
        )

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
