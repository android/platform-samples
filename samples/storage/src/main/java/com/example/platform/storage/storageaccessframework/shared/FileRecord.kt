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

package com.example.platform.storage.storageaccessframework.shared

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FileRecord(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val fileType: FileType,
) {
    companion object {
        suspend fun fromUri(uri: Uri, context: Context): FileRecord? = withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(uri) ?: return@withContext null
            val fileType = when {
                mimeType.startsWith("image/") -> FileType.Image
                mimeType.startsWith("video/") -> FileType.Video
                mimeType.startsWith("audio/") -> FileType.Audio
                mimeType.startsWith("text/") -> FileType.Text
                mimeType == "application/pdf" -> FileType.Pdf
                else -> FileType.Any
            }

            val projection = arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
            )

            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null,
            ) ?: return@withContext null

            cursor.use {
                if (!cursor.moveToFirst()) {
                    return@withContext null
                }

                return@use FileRecord(
                    uri = uri,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)),
                    size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)),
                    mimeType = mimeType,
                    fileType = fileType,
                )
            }
        }
    }
}

enum class FileType(val mimeType: String) {
    Image("image/*"),
    Video("video/*"),
    Audio("audio/*"),
    Text("text/*"),
    Pdf("application/pdf"),
    Any("*/*");
}