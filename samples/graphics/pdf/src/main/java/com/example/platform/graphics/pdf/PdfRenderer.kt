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


package com.example.platform.graphics.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "PdfRenderer",
    description = "Demonstrates how to use PdfRenderer to display PDF documents on the screen."
)
@Composable
fun PdfRendererScreen() {
    val viewModel: PdfRendererViewModel = viewModel()
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val nullableBitmap by viewModel.page.collectAsState()
            nullableBitmap?.let { bitmap ->
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            val currentPage by viewModel.currentPage.collectAsState(initial = "")
            Text(
                text = currentPage,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val previousEnabled by viewModel.previousEnabled.collectAsState(initial = false)
            TextButton(
                onClick = viewModel::previous,
                modifier = Modifier.weight(1f),
                enabled = previousEnabled
            ) {
                Text(
                    text = "PREVIOUS"
                )
            }
            val nextEnabled by viewModel.nextEnabled.collectAsState(initial = false)
            TextButton(
                onClick = viewModel::next,
                modifier = Modifier.weight(1f),
                enabled = nextEnabled
            ) {
                Text(
                    text = "NEXT"
                )
            }
        }
    }
}
