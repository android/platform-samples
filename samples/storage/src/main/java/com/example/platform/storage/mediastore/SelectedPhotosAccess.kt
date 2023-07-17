package com.example.platform.storage.mediastore

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files
import android.provider.MediaStore.Files.FileColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Sample(
    name = "Selected Photos Access",
    description = "Check and request storage permissions",
    documentation = "https://developer.android.com/about/versions/14/changes/partial-photo-video-access",
)
@RequiresPermission(anyOf = [READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE])
@Composable
fun SelectedPhotosAccessScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var files by remember { mutableStateOf(emptyList<FileEntry>()) }

    val storageAccess by produceState(
        initialValue = StorageAccess.Denied,
        context,
        lifecycleOwner,
    ) {
        val eventObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                value = getStorageAccess(context)

                if (value != StorageAccess.Denied) {
                    coroutineScope.launch {
                        files = getVisualMedia(context.contentResolver)
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(eventObserver)
        awaitDispose {
            lifecycleOwner.lifecycle.removeObserver(eventObserver)
        }
    }

    val requestPermissions = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        // Better logic to handle denied & permanently denied access should be written here.
        // We recommend you to look at the {Single Permission} sample
    }

    Column(Modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text("Storage Access") },
            trailingContent = { Text(storageAccess.name) },
        )
        Divider()
        ListItem(
            headlineContent = { Text("Add files to the selection") },
            trailingContent = {
                if (files.isNotEmpty()) {
                    Text("${files.size} items")
                }
            },
            supportingContent = {
                if (storageAccess == StorageAccess.Full) {
                    Text("✅ Access to gallery fully granted")
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TextButton(
                                onClick = {
                                    requestPermissions.launch(
                                        arrayOf(
                                            READ_MEDIA_IMAGES,
                                            READ_MEDIA_VISUAL_USER_SELECTED
                                        )
                                    )
                                },
                            ) {
                                Text("Images")
                            }
                            TextButton(
                                onClick = {
                                    requestPermissions.launch(
                                        arrayOf(
                                            READ_MEDIA_VIDEO,
                                            READ_MEDIA_VISUAL_USER_SELECTED
                                        )
                                    )
                                },
                            ) {
                                Text("Videos")
                            }
                            TextButton(
                                onClick = {
                                    requestPermissions.launch(
                                        arrayOf(
                                            READ_MEDIA_IMAGES,
                                            READ_MEDIA_VIDEO,
                                            READ_MEDIA_VISUAL_USER_SELECTED
                                        )
                                    )
                                },
                            ) {
                                Text("Both")
                            }
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TextButton(
                                onClick = { requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES)) }
                            ) {
                                Text("Images")
                            }
                            TextButton(
                                onClick = { requestPermissions.launch(arrayOf(READ_MEDIA_VIDEO)) }
                            ) {
                                Text("Videos")
                            }
                            TextButton(
                                onClick = {
                                    requestPermissions.launch(
                                        arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
                                    )
                                },
                            ) {
                                Text("Both")
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
                            },
                        ) {
                            Text("Request full gallery access")
                        }
                    }
                }
            },
        )
        Divider()
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items(files) { file ->
                AsyncImage(
                    model = file.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f),
                )
            }
        }
    }
}

/**
 * On Android 14+ devices, users can grant full or partial access to their photo library for apps
 * requesting [READ_MEDIA_IMAGES] and/or [READ_MEDIA_VIDEO] permissions.
 * On older devices, the photo library access can either be full or denied
 */
private enum class StorageAccess {
    Full, Partial, Denied
}

/**
 * Depending on the version of Android the device is running, the app should request the right
 * storage permissions:
 * Up to Android 12L    -> [READ_EXTERNAL_STORAGE]
 * Android 13           -> [READ_MEDIA_IMAGES] and/or [READ_MEDIA_VIDEO]
 * Android 14+          -> Partial access sets only [READ_MEDIA_VISUAL_USER_SELECTED] to granted
 *                      -> Full access sets [READ_MEDIA_IMAGES] and/or [READ_MEDIA_VIDEO] to granted
 */
private fun getStorageAccess(context: Context): StorageAccess {
    return if (
        checkSelfPermission(context, READ_MEDIA_IMAGES) == PERMISSION_GRANTED ||
        checkSelfPermission(context, READ_MEDIA_VIDEO) == PERMISSION_GRANTED
    ) {
        // Full access on Android 13+
        StorageAccess.Full
    } else if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(context, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED
    ) {
        // Partial access on Android 13+
        StorageAccess.Partial
    } else if (checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
        // Full access up to Android 12
        StorageAccess.Full
    } else {
        // Access denied
        StorageAccess.Denied
    }
}

/**
 * Query [MediaStore] through [ContentResolver] to get all images & videos sorted by most added date
 * by targeting the [Files] collection
 */
private suspend fun getVisualMedia(contentResolver: ContentResolver): List<FileEntry> {
    return withContext(Dispatchers.IO) {
        // List of columns we want to fetch
        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.DISPLAY_NAME,
            FileColumns.SIZE,
            FileColumns.MIME_TYPE,
            FileColumns.DATE_ADDED,
        )

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This allows us to query all the device storage volumes instead of the primary only
            Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            Files.getContentUri("external")
        }

        val visualMedia = mutableListOf<FileEntry>()

        contentResolver.query(
            // Queried collection
            collectionUri,
            // List of columns we want to fetch
            projection,
            // Filtering parameters (in this case [MEDIA_TYPE] column)
            "${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?",
            // Filtering values (in this case image or video files)
            arrayOf(
                FileColumns.MEDIA_TYPE_IMAGE.toString(),
                FileColumns.MEDIA_TYPE_VIDEO.toString(),
            ),
            // Sorting order (recent -> older files)
            "${FileColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(FileColumns.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(FileColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                val name = cursor.getString(displayNameColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)

                visualMedia.add(FileEntry(uri, name, size, mimeType, dateAdded))
            }
        }

        return@withContext visualMedia
    }
}