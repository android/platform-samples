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

package com.example.platform.accessibility

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Speakable Text",
    description = "The sample demonstrates the importance of having proper labels for" +
            " interactive elements and how to audit your app for content label related " +
            "improvements.",
    documentation = "https://developer.android.com/guide/topics/ui/accessibility/apps#describe-ui-element"
)

@Preview
@Composable
fun SpeakableText() {
    Column {
        TextWithPadding(
            text = "Users of screen readers rely on content labels to understand the meaning of" +
                    " elements on the screen."
        )

        TextWithPadding(
            text = "Turn on TalkBack to navigate this screen to better understand how proper" +
                    " content labels affect the user experience. You can also run Accessibility" +
                    "Scanner on this screen to see how Scanner provides tips for better labels."
        )

        TextWithPadding(
            text = "Content labels should be succinct and clearly describe the meaning or action" +
                    " associated with the element. It should not include the element's type" +
                    " (ie: button, checkbox), or instruct the user how to specifically interact" +
                    " with the element. Below are examples of bad content labels that degrade the" +
                    " user experience."
        )

        Row(
            Modifier
                .padding(20.dp)
                .align(Alignment.CenterHorizontally)
        ) {

            // Content descriptions should not include the element's type. In this case, including
            // the word "button" is extraneous.
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Edit, contentDescription = "Call Button")
            }

            // Content descriptions should not include instructions on how to specifically
            // interact with the element. In this case, "Double Tap" is extraneous.
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Edit, contentDescription = "Call Button, Double Tap")
            }

            // Content descriptions should describe the action associated with the element instead
            // of a description of the element itself.
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Edit, contentDescription = "Pencil")
            }

            // Elements with a content description make it difficult for users to navigate the app.
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
        }

        TextWithPadding(
            text = "Below is an example of a button with a succinct and descriptive content label."
        )

        Row(
            Modifier
                .padding(20.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}
