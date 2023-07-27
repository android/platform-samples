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


package com.example.platform.storage.mediastore

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.text.format.Formatter
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.platform.base.PermissionBox
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Sample(
    name = "MediaStore - Query",
    description = "Query files indexed by MediaStore",
    documentation = "https://developer.android.com/training/data-storage/shared/media#media_store",
)
@SuppressLint("MissingPermission")
@Composable
fun MediaStoreQueryScreen() {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        READ_MEDIA_IMAGES
    } else {
        READ_EXTERNAL_STORAGE
    }

    PermissionBox(
        permission = permission,
        onGranted = { MediaStoreQueryContent() },
        description = "Permission needed to access the device's photo library",
    )
}

@RequiresPermission(anyOf = [READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE])
@Composable
fun MediaStoreQueryContent() {
    val context = LocalContext.current
    val files by loadImages(context.contentResolver)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = {
                    if (files.isNotEmpty()) {
                        Text("${files.size} images found")
                    }
                },
            )
            Divider()
        }
        items(files) { file ->
            ListItem(
                leadingContent = {
                    AsyncImage(
                        model = file.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .aspectRatio(1f),
                    )
                },
                headlineContent = {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = { Text(file.mimeType) },
                trailingContent = { Text(Formatter.formatShortFileSize(context, file.size)) },
            )
            Divider()
        }
    }
}

@Composable
private fun loadImages(
    contentResolver: ContentResolver,
): State<List<FileEntry>> = produceState(initialValue = emptyList()) {
    value = getImages(contentResolver)
}

/**
 * Query [MediaStore] through [ContentResolver] to get all images sorted by added date by targeting
 * the [Images] collection
 */
private suspend fun getImages(contentResolver: ContentResolver): List<FileEntry> {
    return withContext(Dispatchers.IO) {
        // List of columns we want to fetch
        val projection = arrayOf(
            Images.Media._ID,
            Images.Media.DISPLAY_NAME,
            Images.Media.SIZE,
            Images.Media.MIME_TYPE,
            Images.Media.DATE_ADDED,
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This allows us to query all the device storage volumes instead of the primary only
            Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            Images.Media.EXTERNAL_CONTENT_URI
        }

        val images = mutableListOf<FileEntry>()

        contentResolver.query(
            collectionUri, // Queried collection
            projection, // List of columns we want to fetch
            null, // Filtering parameters (in this case none)
            null, // Filtering values (in this case none)
            "${Images.Media.DATE_ADDED} DESC", // Sorting order (recent -> older files)
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(Images.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(Images.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                images.add(FileEntry(uri, name, size, mimeType, dateAdded))
            }
        }

        return@withContext images
    }
}
