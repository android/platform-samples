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
import android.text.format.Formatter
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.platform.storage.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun FileCard(
    file: FileRecord,
    @DrawableRes iconResourceId: Int,
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
                painter = painterResource(iconResourceId),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
    FileCard(file, R.drawable.ic_image_24) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            var loadThumbnail by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))
            if (loadThumbnail) {
                AsyncImage(
                    model = file.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f),
                )
            } else {
                Button(onClick = { loadThumbnail = true }) {
                    Text("Load thumbnail")
                }
            }
        }
    }
}

@Composable
fun VideoFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, R.drawable.ic_video_file_24) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            var loadThumbnail by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))
            if (loadThumbnail) {
                AsyncImage(
                    model = file.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f),
                )
            } else {
                Button(onClick = { loadThumbnail = true }) {
                    Text("Load thumbnail")
                }
            }
        }
    }
}

@Composable
fun AudioFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, R.drawable.ic_audio_file_24) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            var loadFilePreview by remember { mutableStateOf(false) }
            val filePreview by loadRawFileContent(file.uri, LocalContext.current, loadFilePreview)

            Spacer(modifier = Modifier.height(16.dp))
            when (filePreview) {
                FilePreview.NotLoadedYet -> {
                    Button(onClick = { loadFilePreview = true }) {
                        Text("Display first 10 bytes")
                    }
                }

                FilePreview.Loading -> {
                    Text("Loading...")
                }

                is FilePreview.Loaded -> {
                    Text("First 10 bytes: ${(filePreview as FilePreview.Loaded).content}")
                }

                is FilePreview.Error -> {
                    Text("Error: ${(filePreview as FilePreview.Error).throwable.message}")
                }
            }
        }
    }
}

@Composable
fun TextFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, R.drawable.ic_description_24) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            var loadFilePreview by remember { mutableStateOf(false) }
            val filePreview by loadTextFileContent(file.uri, LocalContext.current, loadFilePreview)

            Spacer(modifier = Modifier.height(16.dp))
            when (filePreview) {
                FilePreview.NotLoadedYet -> {
                    Button(onClick = { loadFilePreview = true }) {
                        Text("Read first 300 characters")
                    }
                }

                FilePreview.Loading -> {
                    Text("Loading...")
                }

                is FilePreview.Loaded -> {
                    Text("First 300 chars: ${(filePreview as FilePreview.Loaded).content}")
                }

                is FilePreview.Error -> {
                    Text("Error: ${(filePreview as FilePreview.Error).throwable.message}")
                }
            }
        }
    }
}

@Composable
fun PdfFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, R.drawable.ic_picture_as_pdf_24) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            var loadFilePreview by remember { mutableStateOf(false) }
            val filePreview by loadRawFileContent(file.uri, LocalContext.current, loadFilePreview)

            Spacer(modifier = Modifier.height(16.dp))
            when (filePreview) {
                FilePreview.NotLoadedYet -> {
                    Button(onClick = { loadFilePreview = true }) {
                        Text("Display first 10 bytes")
                    }
                }

                FilePreview.Loading -> {
                    Text("Loading...")
                }

                is FilePreview.Loaded -> {
                    Text("First 10 bytes: ${(filePreview as FilePreview.Loaded).content}")
                }

                is FilePreview.Error -> {
                    Text("Error: ${(filePreview as FilePreview.Error).throwable.message}")
                }
            }
        }
    }
}

@Composable
fun BinaryFileCard(file: FileRecord, contentPreview: @Composable (() -> Unit)? = null) {
    FileCard(file, R.drawable.ic_insert_drive_file_24) {
        if (contentPreview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            contentPreview()
        } else {
            Spacer(modifier = Modifier.height(16.dp))

            var loadFilePreview by remember { mutableStateOf(false) }
            val filePreview by loadRawFileContent(file.uri, LocalContext.current, loadFilePreview)

            Spacer(modifier = Modifier.height(16.dp))
            when (filePreview) {
                FilePreview.NotLoadedYet -> {
                    Button(onClick = { loadFilePreview = true }) {
                        Text("Display first 10 bytes")
                    }
                }

                FilePreview.Loading -> {
                    Text("Loading...")
                }

                is FilePreview.Loaded -> {
                    Text("First 10 bytes: ${(filePreview as FilePreview.Loaded).content}")
                }

                is FilePreview.Error -> {
                    Text("Error: ${(filePreview as FilePreview.Error).throwable.message}")
                }
            }
        }
    }
}

@Preview
@Composable
fun ImageFileCard_Preview() {
    ImageFileCard(
        FileRecord(
            Uri.EMPTY,
            "AmazingPhoto.png",
            345_000,
            "image/png",
            FileType.Image,
        ),
    )
}

@Preview
@Composable
fun VideoFileCard_Preview() {
    VideoFileCard(
        FileRecord(
            Uri.EMPTY,
            "All hands - meeting recording.mp4",
            1_234_567_890,
            "video/mp4",
            FileType.Video,
        ),
    )
}

@Preview
@Composable
fun AudioFileCard_Preview() {
    AudioFileCard(
        FileRecord(
            Uri.EMPTY,
            "Queen - We will rock you.mp3",
            5_432_100,
            "audio/mp3",
            FileType.Audio,
        ),
    )
}

@Preview
@Composable
fun TextFileCard_Preview() {
    TextFileCard(
        FileRecord(
            Uri.EMPTY,
            "Android Jetpack Compose.txt",
            5_678,
            "text/plain",
            FileType.Text,
        ),
    )
}

@Preview
@Composable
fun PdfFileCard_Preview() {
    PdfFileCard(
        FileRecord(
            Uri.EMPTY,
            "Android Jetpack Compose.pdf",
            1_234_567,
            "application/pdf",
            FileType.Pdf,
        ),
    )
}

@Preview
@Composable
fun BinaryFileCard_Preview() {
    BinaryFileCard(
        FileRecord(
            Uri.EMPTY,
            "binary.bin",
            78_420_968,
            "application/octet-stream",
            FileType.Any,
        ),
    )
}

sealed interface FilePreview {
    data object NotLoadedYet : FilePreview
    data object Loading : FilePreview

    @JvmInline
    value class Loaded(val content: String) : FilePreview

    @JvmInline
    value class Error(val throwable: Throwable) : FilePreview
}

@Composable
fun loadTextFileContent(
    uri: Uri,
    context: Context,
    loadContent: Boolean = false,
    numberOfChars: Int = 300,
): State<FilePreview> {
    return produceState<FilePreview>(FilePreview.NotLoadedYet, uri, loadContent, numberOfChars) {
        withContext(Dispatchers.IO) {
            if (!loadContent) {
                return@withContext
            }

            value = FilePreview.Loading

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val buffer = CharArray(numberOfChars)
                    val charsRead = reader.read(buffer)

                    value = if (charsRead > 0) {
                        FilePreview.Loaded(String(buffer, 0, charsRead))
                    } else {
                        FilePreview.Error(Exception("End of file or no characters available."))
                    }
                }
            } ?: run {
                value = FilePreview.Error(Exception("Failed to open InputStream"))
            }
        }
    }
}

@Composable
fun loadRawFileContent(
    uri: Uri,
    context: Context,
    loadContent: Boolean = false,
    numberOfBytes: Int = 10,
): State<FilePreview> {
    return produceState<FilePreview>(FilePreview.NotLoadedYet, uri, loadContent, numberOfBytes) {
        withContext(Dispatchers.IO) {
            if (!loadContent) {
                return@withContext
            }

            value = FilePreview.Loading

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(numberOfBytes)
                val bytesRead = inputStream.read(buffer)

                value = if (bytesRead > 0) {
                    FilePreview.Loaded(buffer.joinToString(" | ") { byte -> byte.toString() })
                } else {
                    FilePreview.Error(Exception("End of InputStream or no bytes available"))
                }
            } ?: run {
                value = FilePreview.Error(Exception("Failed to open InputStream"))
            }
        }
    }
}