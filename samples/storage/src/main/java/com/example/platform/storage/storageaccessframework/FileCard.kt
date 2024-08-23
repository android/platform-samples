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

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal enum class FileType(val mimeType: String) {
    Image("image/*"),
    Video("video/*"),
    Audio("audio/*"),
    Text("text/*"),
    Pdf("application/pdf"),
    Any("*/*");
}

data class FileRecord(val filename: String, val size: Long, val mimeType: String)

@Composable
fun FileCard(
    file: FileRecord,
    icon: ImageVector,
    contentPreview: @Composable (() -> Unit)? = null,
) {
    val sizeLabel = Formatter.formatShortFileSize(LocalContext.current, file.size)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(file.filename, style = MaterialTheme.typography.headlineSmall, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$sizeLabel · ${file.mimeType}", style = MaterialTheme.typography.bodyMedium)

                if (contentPreview != null) {
                    contentPreview()
                }
            }
        }
    }
}

@Composable
fun ImageFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, Icons.Default.Image) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Handle button click */ }) {
                Text("Read first 10 bytes")
            }
        }
    }
}

@Composable
fun VideoFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, Icons.Default.VideoFile) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Handle button click */ }) {
                Text("Read first 10 bytes")
            }
        }
    }
}

@Composable
fun AudioFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, Icons.Default.AudioFile) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Handle button click */ }) {
                Text("Read first 10 bytes")
            }
        }
    }
}

@Composable
fun TextFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, Icons.Default.Description) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Handle button click */ }) {
                Text("Read first 10 bytes")
            }
        }
    }
}

@Composable
fun PdfFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, Icons.Default.PictureAsPdf) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Handle button click */ }) {
                Text("Read first 10 bytes")
            }
        }
    }
}

@Composable
fun BinaryFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, Icons.AutoMirrored.Filled.InsertDriveFile) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Handle button click */ }) {
                Text("Read first 10 bytes")
            }
        }
    }
}

@Preview
@Composable
fun ImageFileCard_Preview() {
    ImageFileCard(FileRecord("AmazingPhoto.png", 345_000, "image/png"))
}

@Preview
@Composable
fun VideoFileCard_Preview() {
    VideoFileCard(FileRecord("All hands - meeting recording.mp4", 1_234_567_890, "image/png"))
}

@Preview
@Composable
fun AudioFileCard_Preview() {
    AudioFileCard(FileRecord("Queen - We will rock you.mp3", 5_432_100, "audio/mp3"))
}

@Preview
@Composable
fun TextFileCard_Preview() {
    TextFileCard(FileRecord("Android Jetpack Compose.txt", 5_678, "text/plain"))
}

@Preview
@Composable
fun PdfFileCard_Preview() {
    PdfFileCard(FileRecord("Android Jetpack Compose.pdf", 1_234_567, "application/pdf"))
}

@Preview
@Composable
fun BinaryFileCard_Preview() {
    BinaryFileCard(FileRecord("binary.bin", 78_420_968, "application/octet-stream"))
}