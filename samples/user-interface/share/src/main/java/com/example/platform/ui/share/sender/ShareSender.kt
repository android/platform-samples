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

package com.example.platform.ui.share.sender

import android.app.PendingIntent
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.example.platform.ui.share.R
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Send data with sharesheet",
    description = "Send texts and images to other apps using the Android Sharesheet.",
)
@Composable
fun ShareSender() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        val context = LocalContext.current
        Title(text = "Send various data")
        ShareButton(text = "Share plain text") { sharePlainText(context) }
        ShareButton(text = "Share rich text") { shareRichText(context) }
        ShareButton(text = "Share image") { shareImage(context) }
        ShareButton(text = "Share multiple images") { shareMultipleImages(context) }
        Spacer(modifier = Modifier.height(16.dp))
        Title(text = "Sharesheet")
        ShareButton(text = "Add or reorder targets") { addOrReorderTargets(context) }
        if (Build.VERSION.SDK_INT >= 22) {
            ShareButton(text = "Know which app has received the data") { knowReceiver(context) }
        }
        if (Build.VERSION.SDK_INT >= 24) {
            ShareButton(text = "Exclude target candidates") { excludeTarget(context) }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            ShareButton(text = "Add custom actions") { addCustomAction(context) }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
private fun ShareButton(text: String, action: () -> Unit) {
    OutlinedButton(
        onClick = action,
        modifier = Modifier.padding(start = 16.dp),
    ) {
        Text(text = text)
    }
}

private val filenames = listOf(
    "grand_canyon.jpg",
    "lamps.jpg",
    "office.jpg",
    "train_station_night.jpg",
    "night_highrise.jpg",
    "night_highrise.jpg",
)

private fun sharePlainText(context: Context) {
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText("Hello, world!")
            .createChooserIntent(),
    )
}

private fun shareRichText(context: Context) {
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("text/rtf")
            .setText(
                // Rich texts can be shared as SpannableString.
                buildSpannedString {
                    color(android.graphics.Color.BLUE) {
                        append("Hello")
                    }
                    append(", ")
                    bold {
                        append("world")
                    }
                    append("!")
                },
            )
            .createChooserIntent(),
    )
}

private fun shareImage(context: Context) {
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("image/jpeg")
            // Images and other files are shared via URIs starting with "content://...".
            // ShareCompat handles the permission automatically for you.
            // The selected target app can access the shared file, but no one else can.
            .setStream(AssetFileProvider.getUriForFilename(filenames[0]))
            .createChooserIntent(),
    )
}

private fun shareMultipleImages(context: Context) {
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("image/jpeg")
            .apply {
                for (filename in filenames) {
                    addStream(AssetFileProvider.getUriForFilename(filename))
                }
            }
            .createChooserIntent(),
    )
}

private fun addOrReorderTargets(context: Context) {
    // Sharesheet allows the sending app to specify "initial intents".
    // If the intent designates one of the available targets, it is raised to the top of the list.
    // Otherwise, the intent is prepended to the list.
    val intent = Intent(Intent.ACTION_SEND)
        .setComponent(
            ComponentName(
                "com.example.platform",
                "com.example.platform.ui.share.receiver.ReceiverActivity",
            ),
        )
        // You can also use this feature to adjust parameters specifically for the target.
        .putExtra(Intent.EXTRA_TEXT, "It is fine today.")
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText("Hello, world!")
            .createChooserIntent()
            .putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent)),
    )
}

@RequiresApi(22)
private fun knowReceiver(context: Context) {
    val sendIntent = ShareCompat.IntentBuilder(context)
        .setType("text/plain")
        .setText("Hello, world!")
        .intent
    // We can set up a BroadcastReceiver to be notified who has received the data we shared.
    val sender = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, ShareResultReceiver::class.java),
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val chooserIntent = Intent.createChooser(sendIntent, null, sender.intentSender)
    context.startActivity(chooserIntent)
}

@RequiresApi(24)
private fun excludeTarget(context: Context) {
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText("Hello, world!")
            .createChooserIntent()
            // We can specify targets to be excluded from the sharesheet.
            .putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(
                    ComponentName(
                        "com.example.platform",
                        "com.example.platform.ui.share.receiver.ReceiverActivity",
                    ),
                ),
            ),
    )
}

@RequiresApi(34)
private fun addCustomAction(context: Context) {
    val text = "Hello, world!"
    // Add custom actions to provide additional features on the sharesheet.
    // The action can be anything as far as it can be represented as a PendingIntent.
    val customAction = ChooserAction
        .Builder(
            Icon.createWithResource(context, R.drawable.ic_search),
            "Search on web",
            PendingIntent.getActivity(
                context,
                0,
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()
    context.startActivity(
        ShareCompat.IntentBuilder(context)
            .setType("text/plain")
            .setText(text)
            .createChooserIntent()
            .putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, arrayOf(customAction)),
    )
}
