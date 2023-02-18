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

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.catalog.framework.annotations.Sample

@RequiresApi(Build.VERSION_CODES.Q)
@Sample(
    name = "Location - Permissions",
    description = "This Sample demonstrate best practices for Location Permission"
)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionScreen() {

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val fineLocationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val bgLocationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val context = LocalContext.current

    var showRationale by remember { mutableStateOf(false) }
    var rationaleState by remember {
        mutableStateOf(RationaleState())
    }
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showRationale) {
                ShowRationale(rationaleState)
            }
            PermissionRequestCard(
                isGranted = locationPermissionState.status.isGranted,
                title = "Approximate location access"
            ) {
                if (locationPermissionState.status.shouldShowRationale) {
                    showRationale = true
                    rationaleState = RationaleState(
                        "Request approximate location access",
                        "In order to use this feature please grant access by accepting " + "the location permission dialog." + "\n\nWould you like to continue?"
                    ) {
                        if (it) {
                            locationPermissionState.launchPermissionRequest()
                        }
                        showRationale = false
                    }
                } else {
                    locationPermissionState.launchPermissionRequest()
                }
            }

            PermissionRequestCard(
                isGranted = fineLocationPermissionState.allPermissionsGranted,
                title = "Precise location access"
            ) {
                if (fineLocationPermissionState.shouldShowRationale) {
                    showRationale = true
                    rationaleState = RationaleState(
                        "Request Precise Location",
                        "In order to use this feature please grant access by accepting " + "the location permission dialog." + "\n\nWould you like to continue?"
                    ) {
                        if (it) {
                            fineLocationPermissionState.launchMultiplePermissionRequest()
                        }
                        showRationale = false
                    }
                } else {
                    fineLocationPermissionState.launchMultiplePermissionRequest()
                }
            }

            // Background location permission needed from Android Q,
            // before Android Q, granting Fine or Coarse location access automatically grants Background
            // location access
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PermissionRequestCard(
                    isGranted = bgLocationPermissionState.status.isGranted,
                    title = "Background location access"
                ) {
                    if (locationPermissionState.status.isGranted || fineLocationPermissionState.allPermissionsGranted) {
                        if (bgLocationPermissionState.status.shouldShowRationale) {
                            showRationale = true
                            rationaleState = RationaleState(
                                "Request background location",
                                "In order to use this feature please grant access by accepting " + "the background location permission dialog." + "\n\nWould you like to continue?"
                            ) {
                                if (it) {
                                    bgLocationPermissionState.launchPermissionRequest()
                                }
                                showRationale = false
                            }
                        } else {
                            bgLocationPermissionState.launchPermissionRequest()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please grant either Approximate location access permission or Fine" + "location access permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        FloatingActionButton(modifier = Modifier.align(Alignment.BottomEnd),
            onClick = { context.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS)) }) {
            Icon(Icons.Outlined.Settings, "App Settings")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestCard(isGranted: Boolean, title: String, onClick: () -> Unit) {
    if (isGranted) {
        PermissionInfo(title)
    } else {
        Button(onClick = onClick) {
            Text("Request $title")
        }
    }
}

@Composable
fun PermissionInfo(title: String) {
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
}

@Composable
fun ShowRationale(rationaleState: RationaleState) {
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
    val onRationaleReply: (Boolean) -> Unit,
) {
    constructor() : this("", "", {})
}
