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

package com.example.platform.location.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Simple screen that manages the location permission state
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissions(text: String, rationale: String, locationState: PermissionState) {
    LocationPermissions(
        text = text,
        rationale = rationale,
        locationState = rememberMultiplePermissionsState(
            permissions = listOf(
                locationState.permission
            )
        )
    )
}

/**
 * Simple screen that manages the location permission state
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissions(text: String, rationale: String, locationState: MultiplePermissionsState) {
    var showRationale by remember(locationState) {
        mutableStateOf(false)
    }
    if (showRationale) {
        PermissionRationaleDialog(rationaleState = RationaleState(
            title = "Location Permission Access",
            rationale = rationale,
            onRationaleReply = { proceed ->
                if (proceed) {
                    locationState.launchMultiplePermissionRequest()
                }
                showRationale = false
            }
        ))
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PermissionRequestButton(isGranted = false, title = text) {
            if (locationState.shouldShowRationale) {
                showRationale = true
            } else {
                locationState.launchMultiplePermissionRequest()
            }
        }
    }
}

/**
 * A button that shows the title or the request permission action.
 */
@Composable
fun PermissionRequestButton(isGranted: Boolean, title: String, onClick: () -> Unit) {
    if (isGranted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CheckCircle, title, modifier = Modifier.size(48.dp))
            Spacer(Modifier.size(10.dp))
            Text(text = title, modifier = Modifier.background(Color.Transparent))
        }
    } else {
        Button(onClick = onClick) {
            Text("Request $title")
        }
    }
}

/**
 * Simple AlertDialog that displays the given rationale state
 */
@Composable
fun PermissionRationaleDialog(rationaleState: RationaleState) {
    AlertDialog(onDismissRequest = { rationaleState.onRationaleReply(false) }, title = {
        Text(text = rationaleState.title)
    }, text = {
        Text(text = rationaleState.rationale)
    }, confirmButton = {
        TextButton(onClick = {
            rationaleState.onRationaleReply(true)
        }) {
            Text("Continue")
        }
    }, dismissButton = {
        TextButton(onClick = {
            rationaleState.onRationaleReply(false)
        }) {
            Text("Dismiss")
        }
    })
}

data class RationaleState(
    val title: String,
    val rationale: String,
    val onRationaleReply: (proceed: Boolean) -> Unit,
)