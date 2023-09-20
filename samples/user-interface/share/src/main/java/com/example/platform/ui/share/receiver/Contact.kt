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

import android.net.Uri
import androidx.core.net.toUri

private const val shortcutIdPrefix = "contact-"

data class Contact(
    val id: Long,
    val name: String,
    val filename: String,
) {
    companion object

    val shortcutId: String
        get() = "$shortcutIdPrefix$id"

    val contentUri: Uri
        get() = "https://platform.example.com/receiver/contact/$id".toUri()
}

val Contact.Companion.All: List<Contact>
    get() = listOf(
        Contact(id = 1L, name = "Cat", filename = "contact/cat.jpg"),
        Contact(id = 2L, name = "Dog", filename = "contact/dog.jpg"),
        Contact(id = 3L, name = "Parrot", filename = "contact/parrot.jpg"),
        Contact(id = 4L, name = "Sheep", filename = "contact/sheep.jpg"),
    )

fun Contact.Companion.fromShortcutId(shortcutId: String): Contact? {
    if (!shortcutId.startsWith(shortcutIdPrefix)) return null
    val id = shortcutId.substring(shortcutIdPrefix.length).toLongOrNull() ?: return null
    return All.find { it.id == id }
}
