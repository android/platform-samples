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

package com.example.platform.storage.storageaccessframework

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.platform.storage.R
import com.example.platform.storage.storageaccessframework.shared.AudioFileCard
import com.example.platform.storage.storageaccessframework.shared.BinaryFileCard
import com.example.platform.storage.storageaccessframework.shared.FileRecord
import com.example.platform.storage.storageaccessframework.shared.FileType
import com.example.platform.storage.storageaccessframework.shared.ImageFileCard
import com.example.platform.storage.storageaccessframework.shared.PdfFileCard
import com.example.platform.storage.storageaccessframework.shared.TextFileCard
import com.example.platform.storage.storageaccessframework.shared.VideoFileCard
import kotlinx.coroutines.launch

@Composable
fun GetContentSample() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(FileType.Any) }
    var selectMultiple by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf(emptyList<FileRecord>()) }

    val getSingleDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            coroutineScope.launch {
                selectedFiles = uri?.let { uri ->
                    FileRecord.fromUri(uri, context)?.let { listOf(it) }
                } ?: emptyList()
            }
        }

    val getMultipleDocuments =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            coroutineScope.launch {
                selectedFiles = uris.mapNotNull { uri ->
                    FileRecord.fromUri(uri, context)
                }
            }
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectMultiple) {
                        getMultipleDocuments.launch(selectedFilter.mimeType)
                    } else {
                        getSingleDocument.launch(selectedFilter.mimeType)
                    }
                },
            ) {
                Text(if (selectMultiple) "Select Files" else "Select File")
            }
        },
    ) { paddingValues ->
        LazyColumn(Modifier.padding(paddingValues)) {
            item {
                ListItem(
                    headlineContent = { Text("File type filter") },
                    supportingContent = {
                        Text(selectedFilter.name)
                    },
                    trailingContent = {
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .wrapContentSize(Alignment.TopStart),
                        ) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_filter_alt_24),
                                    contentDescription = "Localized description",
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                scrollState = scrollState,
                            ) {
                                FileType.entries.forEach { fileType ->
                                    DropdownMenuItem(
                                        text = { Text(fileType.name) },
                                        onClick = { selectedFilter = fileType },
                                        leadingIcon = {
                                            if (selectedFilter == fileType) {
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    contentDescription = "Selected",
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                            LaunchedEffect(expanded) {
                                if (expanded) {
                                    // Scroll to show the bottom menu items.
                                    scrollState.scrollTo(scrollState.maxValue)
                                }
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
            item {
                ListItem(
                    headlineContent = { Text("Select multiple files?") },
                    trailingContent = {
                        Switch(
                            modifier = Modifier.semantics {
                                contentDescription = "Select multiple files"
                            },
                            checked = selectMultiple,
                            onCheckedChange = { selectMultiple = it },
                            thumbContent = {
                                if (selectMultiple) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            },
                        )
                    },
                )
                HorizontalDivider()
            }
            items(selectedFiles) { file ->
                when (file.fileType) {
                    FileType.Image -> ImageFileCard(file)
                    FileType.Video -> VideoFileCard(file)
                    FileType.Audio -> AudioFileCard(file)
                    FileType.Text -> TextFileCard(file)
                    FileType.Pdf -> PdfFileCard(file)
                    FileType.Any -> BinaryFileCard(file)
                }
            }
        }
    }
}