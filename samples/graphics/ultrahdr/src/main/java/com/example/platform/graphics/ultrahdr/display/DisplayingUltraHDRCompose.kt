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

import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.example.platform.graphics.ultrahdr.databinding.DisplayingUltrahdrBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Displaying UltraHDR (Compose)",
    description = "This sample demonstrates displaying an UltraHDR image in a Compose View .",
    documentation = "https://developer.android.com/guide/topics/media/hdr-image-format",
    tags = ["UltraHDR", "Compose"],
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class DisplayingUltraHDRCompose : Fragment() {
    /**
     *  Android ViewBinding.
     */
    private var _binding: DisplayingUltrahdrBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                rendercomposeView()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        binding.colorModeControls.detach()
    }

}

@Composable
fun rendercomposeView() {

    val ultra_hdr_image = "gainmaps/night_highrise.jpg"
    val stream = LocalContext.current.assets.open(ultra_hdr_image)
    val bitmap = BitmapFactory.decodeStream(stream)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

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

        Text(text = "ImageView")
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = {
                ImageView(it).apply {
                    setImageBitmap(bitmap)
                }
            },
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun Preview1() {
    rendercomposeView()
}
