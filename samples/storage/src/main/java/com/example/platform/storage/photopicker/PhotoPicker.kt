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

package com.example.platform.storage.photopicker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.SingleMimeType
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VideoOnly
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.catalog.framework.annotations.Sample

/**
 * This sample showcases how to select images/videos in a privacy-friendly way using the photo
 * picker. Check out the AndroidManifest.xml of the storage section to see the <service> declaration
 * that enables backport support on Android KitKat onwards using Google Play Services
 */
@OptIn(ExperimentalMaterial3Api::class)
@Sample(
    name = "PhotoPicker",
    description = "Select images/videos in a privacy-friendly way using the photo picker",
    documentation = "https://developer.android.com/training/data-storage/shared/photopicker",
)
@Composable
fun PhotoPicker() {
    var selectedMedia by remember { mutableStateOf(emptyList<Uri>()) }
    // Keep track of the media type filter for the photo picker
    var filter by remember { mutableStateOf<VisualMediaType>(ImageAndVideo) }
    // Keep track of the maximum number of items we want to allow per selection
    var maxItems by remember { mutableStateOf(5) }

    /**
     * [PickMultipleVisualMedia] is an ActivityResultContract that will launch the photo picker
     * intent while providing an enhanced API over the available customisations options (media type,
     * max number of items).
     *
     * If you're looking for single selection only, use PickVisualMedia ActivityResultContract
     * instead.
     */
    val pickMultipleMedia =
        rememberLauncherForActivityResult(PickMultipleVisualMedia(maxItems)) { uris ->
            selectedMedia = uris
        }

    Column(Modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text("Filter") },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    PICKER_FILTERS.forEach { entry ->
                        FilterChip(
                            selected = filter == entry.value,
                            onClick = { filter = entry.value },
                            label = { Text(entry.key) },
                            leadingIcon = if (filter == entry.value) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Selected option",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            },
        )
        Divider()
        ListItem(
            headlineContent = { Text("Max items") },
            trailingContent = {
                Button(onClick = { pickMultipleMedia.launch(PickVisualMediaRequest(filter)) }) {
                    Text("Add media")
                }
            },
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    MAX_ITEMS.forEach { value ->
                        FilterChip(
                            selected = maxItems == value,
                            onClick = { maxItems = value },
                            label = { Text(value.toString()) },
                            leadingIcon = if (maxItems == value) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Selected option",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            },
        )
        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(selectedMedia) { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f),
                )
            }
        }
    }
}


private val PICKER_FILTERS = mapOf(
    "All" to ImageAndVideo,
    "Image" to ImageOnly,
    "Video" to VideoOnly,
    "GIF" to SingleMimeType("image/gif"),
)

private val MAX_ITEMS = listOf(2, 5, 10)