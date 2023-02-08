package com.example.platform.privacy.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.database.getStringOrNull
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@OptIn(ExperimentalTextApi::class)
@Sample(
    name = "Permissionless",
    description = "This sample demonstrate how you can avoid requesting permission for certain actions by leveraging System APIs",
    documentation = "https://developer.android.com/training/permissions/evaluating"
)
@Composable
fun Permissionless() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TitleCard()
        }
        item {
            TakePhoto()
        }
        item {
            CaptureVideo()
        }
        item {
            PickContact()
        }
        item {
            PickMedia()
        }
        item {
            OpenDocuments()
        }
    }
}

@Composable
@OptIn(ExperimentalTextApi::class)
private fun TitleCard() {
    val uriHandler = LocalUriHandler.current
    val title = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
            )
        ) {
            append("The following actions don't require any permission (")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                val startIndex = this.length
                append("more actions")
                addUrlAnnotation(
                    UrlAnnotation("https://developer.android.com/training/permissions/evaluating"),
                    start = startIndex,
                    end = this.length
                )
            }
            append(").")
        }
    }
    ClickableText(text = title, style = MaterialTheme.typography.bodyLarge) {
        val url = title.getUrlAnnotations(it, it).firstOrNull()?.item?.url ?: return@ClickableText
        uriHandler.openUri(url)
    }
}

/**
 * Create a file in the cacheDir and returns an shareable URI from the FileProvider as defined in the
 * AndroidManifest.xml.
 *
 * In a real world app you would move this into another layer like a ViewModel or a Repository.
 *
 * Important: there are different ways to create the URI based on your needs, see the table in
 * https://developer.android.com/training/data-storage
 */
private suspend fun Context.createTemporaryFile(name: String, type: String): Uri =
    withContext(Dispatchers.IO) {
        val file = File.createTempFile(name, type, cacheDir).apply {
            createNewFile()
        }
        FileProvider.getUriForFile(applicationContext, "$packageName.provider", file)
    }

/**
 * Register the provided camera related contract and display the UI to request and show the returned
 * value from the camera.
 */
@Composable
private fun CameraRequest(
    actionTitle: String,
    fileName: String,
    fileType: String,
    contract: ActivityResultContract<Uri, Boolean>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Holds the URI generated for the Camera request
    var uri by rememberSaveable {
        mutableStateOf(Uri.EMPTY)
    }

    // Hold the Coil requests to load the provided uri if any
    var imageRequest by rememberSaveable(stateSaver = Saver<ImageRequest?, Uri>(
        save = { it?.data as? Uri },
        restore = { ImageRequest.Builder(context).data(it).build() }
    )) {
        mutableStateOf(null)
    }

    // Create the launcher for the given contract and handle result
    val launcher = rememberLauncherForActivityResult(contract) { isSuccessful ->
        // Create a new image request with the previously create URI to display the URI
        imageRequest = if (isSuccessful) {
            ImageRequest.Builder(context).data(uri).build()
        } else {
            null
        }
        Toast.makeText(context, "Captured? $isSuccessful", Toast.LENGTH_SHORT).show()
    }

    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    // On click create a new file (outside of the main thread!) and launch the camera
                    // request with the new file URI
                    uri = context.createTemporaryFile(fileName, fileType)
                    launcher.launch(uri)
                }
            }
        ) {
            Text(text = actionTitle)
        }
        if (imageRequest != null) {
            // Using [Coil](https://github.com/coil-kt/coil) to load images from URIs
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .clickable {
                        // Request system to display URI
                        val intent = Intent.parseUri(uri.toString(), Intent.URI_ANDROID_APP_SCHEME)
                        // Important: flag needed to allow other apps read internal app's files.
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(intent)
                    }
            )
        }
    }
}

@Composable
private fun TakePhoto() {
    CameraRequest(
        actionTitle = "Take a photo",
        fileName = "sample-photo",
        fileType = ".jpeg",
        contract = ActivityResultContracts.TakePicture()
    )
}

@Composable
private fun CaptureVideo() {
    CameraRequest(
        actionTitle = "Capture video",
        fileName = "sample-video",
        fileType = ".mp4",
        contract = ActivityResultContracts.CaptureVideo()
    )
}

@Composable
private fun PickContact() {
    val context = LocalContext.current
    var contactInfo by rememberSaveable {
        mutableStateOf("")
    }

    // Note: PickContact does not provide all contact information, if you need certain information,
    // like phone number or email, you should create your own contract and launch the intent with
    // ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) {
            Toast.makeText(context, "No contact selected", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        context.contentResolver.query(uri, null, null, null).use { cursor ->
            // If the cursor returned is valid, get the phone number and (or) name
            contactInfo = if (cursor != null && cursor.moveToFirst()) {
                (0 until cursor.columnCount).joinToString("\n") {
                    "${cursor.getColumnName(it)}: ${cursor.getStringOrNull(it)}"
                }
            } else {
                "Error while retrieving contact information"
            }
        }
    }
    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                launcher.launch(null)
            }
        ) {
            Text(text = "Pick contact")
        }

        if (contactInfo.isNotBlank()) {
            Text(text = contactInfo, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun PickMedia() {
    var selectedUri by rememberSaveable {
        mutableStateOf<Uri>(Uri.EMPTY)
    }

    // Create the launcher for the given contract and handle result
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Show the image if the user selected any.
            selectedUri = uri ?: return@rememberLauncherForActivityResult
        }

    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Text(text = "Pick Image")
        }
        if (selectedUri != Uri.EMPTY) {
            // Using [Coil](https://github.com/coil-kt/coil) to load images from URIs
            AsyncImage(
                model = selectedUri,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun OpenDocuments() {
    val context = LocalContext.current
    var documentInfo by rememberSaveable {
        mutableStateOf("")
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                Toast.makeText(context, "No document selected", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                documentInfo = if (cursor != null && cursor.moveToFirst()) {
                    (0 until cursor.columnCount).joinToString("\n") {
                        "${cursor.getColumnName(it)}: ${cursor.getStringOrNull(it)}"
                    }
                } else {
                    "Error while retrieving file information"
                }
            }
        }
    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Request the user to pick a file
                // Note: You can provide multiple mime types to filter results.
                launcher.launch(arrayOf("*/*"))
            }
        ) {
            Text(text = "Open document")
        }

        if (documentInfo.isNotBlank()) {
            Text(text = documentInfo, modifier = Modifier.padding(16.dp))
        }
    }
}