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

package com.example.platform.privacy.permissions.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Contains the values to display the [PermissionScreen]
 */
data class PermissionScreenState(
    val title: String,
    val buttonText: String,
    val errorText: String? = null,
    val rationale: String? = null,
)

/**
 * Common screen UI to showcase permission samples
 */
@Composable
internal fun PermissionScreen(
    state: PermissionScreenState,
    onClick: () -> Unit,
    onRationaleReply: (Boolean) -> Unit
) {
    with(state) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = onClick) {
                    Text(text = buttonText)
                }
                if (errorText != null) {
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            if (rationale != null) {
                AlertDialog(
                    onDismissRequest = { onRationaleReply(false) },
                    title = {
                        Text(text = title)
                    },
                    text = {
                        Text(text = rationale)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onRationaleReply(true)
                            }
                        ) {
                            Text("Continue")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                onRationaleReply(false)
                            }
                        ) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}