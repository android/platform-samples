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

package com.example.platform.ui.share.receiver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.Person
import androidx.core.app.ShareCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.platform.ui.share.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiver(
    intent: Intent,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.receive_share)) }) },
    ) { innerPadding ->
        val context = LocalContext.current
        val intentReader = remember(intent) { ShareCompat.IntentReader(context, intent) }
        var isConsumed by rememberSaveable { mutableStateOf(false) }
        val targetContact = remember(intent) {
            val shortcutId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
            if (shortcutId != null) {
                Contact.fromShortcutId(shortcutId)
            } else {
                null
            }
        }
        val coroutineScope = rememberCoroutineScope()
        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ReceivedDataCard(
                    intentReader = intentReader,
                    targetContact = targetContact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            item {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(items = Contact.All) { contact ->
                val isSharing = if (targetContact == null) {
                    intentReader.isShareIntent && !isConsumed
                } else {
                    targetContact.id == contact.id && !isConsumed
                }
                ContactCard(
                    contact = contact,
                    isSharing = isSharing,
                    onButtonClick = {
                        coroutineScope.launch {
                            pushShortcut(contact, context, isSharing)
                            if (isSharing) {
                                isConsumed = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

private val ShareCompat.IntentReader.streams: List<Uri>
    get() = buildList {
        for (i in 0 until streamCount) {
            add(getStream(i) ?: break)
        }
    }

@Composable
private fun ReceivedDataCard(
    intentReader: ShareCompat.IntentReader,
    targetContact: Contact?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!intentReader.isShareIntent) {
                Text(text = "Nothing received.")
                Text(text = "Use the \"Send data with sharesheet\" sample to share some data with \"Platform Samples\".")
            } else if (intentReader.type?.startsWith("text/") == true) {
                Text(text = "Received a text:")
                intentReader.text?.let { RoundedCornerText(it) }
            } else {
                // We received streams (content URIs).
                val streams = intentReader.streams
                Text(
                    text = if (streams.size == 1) {
                        "Received an image:"
                    } else {
                        "Received images:"
                    },
                )
                Streams(streams = streams)
                intentReader.text?.let { RoundedCornerText(it) }
            }
            if (targetContact != null) {
                Text(text = "Directly shared to:")
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val context = LocalContext.current
                    ContactIcon(context = context, filename = targetContact.filename)
                    Text(
                        text = targetContact.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundedCornerText(text: CharSequence, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = text.toString(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun Streams(
    streams: List<Uri>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items = streams) { stream ->
            Image(
                painter = contentUriPainter(context = context, uri = stream),
                contentDescription = "",
                modifier = Modifier.height(128.dp),
            )
        }
    }
}

@Composable
private fun contentUriPainter(
    context: Context,
    uri: Uri,
    placeholder: Painter = ColorPainter(MaterialTheme.colorScheme.secondary),
    error: Painter = ColorPainter(MaterialTheme.colorScheme.error),
): Painter {
    val state = produceState(initialValue = placeholder) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    value = BitmapPainter(image = bitmap.asImageBitmap())
                }
            } catch (e: FileNotFoundException) {
                value = error
            }
        }
    }
    return state.value
}

@Composable
private fun ContactCard(
    contact: Contact,
    isSharing: Boolean,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            val context = LocalContext.current
            ContactIcon(context = context, filename = contact.filename)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isSharing) {
                    Button(onClick = onButtonClick) {
                        Text(text = "Send shared data to this contact")
                    }
                } else {
                    OutlinedButton(onClick = onButtonClick) {
                        Text(text = "Push a shortcut for this contact")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactIcon(context: Context, filename: String, modifier: Modifier = Modifier) {
    Image(
        painter = rememberAssetPainter(context, filename),
        contentDescription = "",
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape),
    )
}

@Composable
private fun rememberAssetPainter(
    context: Context,
    filename: String,
    placeholder: Painter = ColorPainter(MaterialTheme.colorScheme.secondary),
): Painter {
    val state = produceState(initialValue = placeholder) {
        withContext(Dispatchers.IO) {
            val bitmap = context.resources.assets.open(filename).use { input ->
                BitmapFactory.decodeStream(input)
            }
            value = BitmapPainter(image = bitmap.asImageBitmap())
        }
    }
    return state.value
}

private suspend fun pushShortcut(
    contact: Contact,
    context: Context,
    outgoing: Boolean,
) = withContext(Dispatchers.IO) {
    val icon = IconCompat.createWithAdaptiveBitmap(
        context.resources.assets.open(contact.filename).use { input ->
            BitmapFactory.decodeStream(input)
        },
    )
    val person = Person.Builder()
        .setName(contact.name)
        .setIcon(icon)
        .build()
    val intent = Intent(context, ShareReceiverActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .setData(contact.contentUri)
    val shortcutInfo = ShortcutInfoCompat.Builder(context, contact.shortcutId)
        .setLongLived(true)
        .setCategories(hashSetOf("com.example.platform.receiver.category.SHARE_TARGET"))
        .setActivity(ComponentName(context, ShareReceiverActivity::class.java))
        .setLocusId(LocusIdCompat(contact.shortcutId))
        .setShortLabel(contact.name)
        .setIcon(icon)
        .setIntent(intent)
        .setPerson(person)
        .addCapabilityBinding(
            if (outgoing) {
                "actions.intent.SEND_MESSAGE"
            } else {
                "actions.intent.RECEIVE_MESSAGE"
            },
        )
        .build()
    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)
}
