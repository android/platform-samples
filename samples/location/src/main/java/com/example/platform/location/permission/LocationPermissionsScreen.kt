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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    description = "This Sample demonstrate best practices for Location Permission",
    documentation = "https://developer.android.com/training/location/permissions",
    tags = ["permissions"],
)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionScreen() {
    val context = LocalContext.current

    // Approximate location access is sufficient for most of use cases 
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    // When precision is important request both permissions but make sure to handle the case where
    // the user only grants ACCESS_COARSE_LOCATION
    val fineLocationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
        ),
    )

    // In really rare use cases, accessing background location might be needed.
    val bgLocationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    )

    // Keeps track of the rationale dialog state, needed when the user requires further rationale
    var rationaleState by remember {
        mutableStateOf<RationaleState?>(null)
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Show rationale dialog when needed
            rationaleState?.run { PermissionRationaleDialog(rationaleState = this) }

            PermissionRequestButton(
                isGranted = locationPermissionState.status.isGranted,
                title = "Approximate location access",
            ) {
                if (locationPermissionState.status.shouldShowRationale) {
                    rationaleState = RationaleState(
                        "Request approximate location access",
                        "In order to use this feature please grant access by accepting " + "the location permission dialog." + "\n\nWould you like to continue?",
                    ) { proceed ->
                        if (proceed) {
                            locationPermissionState.launchPermissionRequest()
                        }
                        rationaleState = null
                    }
                } else {
                    locationPermissionState.launchPermissionRequest()
                }
            }

            PermissionRequestButton(
                isGranted = fineLocationPermissionState.allPermissionsGranted,
                title = "Precise location access",
            ) {
                if (fineLocationPermissionState.shouldShowRationale) {
                    rationaleState = RationaleState(
                        "Request Precise Location",
                        "In order to use this feature please grant access by accepting " + "the location permission dialog." + "\n\nWould you like to continue?",
                    ) { proceed ->
                        if (proceed) {
                            fineLocationPermissionState.launchMultiplePermissionRequest()
                        }
                        rationaleState = null
                    }
                } else {
                    fineLocationPermissionState.launchMultiplePermissionRequest()
                }
            }

            // Background location permission needed from Android Q,
            // before Android Q, granting Fine or Coarse location access automatically grants Background
            // location access
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PermissionRequestButton(
                    isGranted = bgLocationPermissionState.status.isGranted,
                    title = "Background location access",
                ) {
                    if (locationPermissionState.status.isGranted || fineLocationPermissionState.allPermissionsGranted) {
                        if (bgLocationPermissionState.status.shouldShowRationale) {
                            rationaleState = RationaleState(
                                "Request background location",
                                "In order to use this feature please grant access by accepting " + "the background location permission dialog." + "\n\nWould you like to continue?",
                            ) { proceed ->
                                if (proceed) {
                                    bgLocationPermissionState.launchPermissionRequest()
                                }
                                rationaleState = null
                            }
                        } else {
                            bgLocationPermissionState.launchPermissionRequest()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please grant either Approximate location access permission or Fine" + "location access permission",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            onClick = { context.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS)) },
        ) {
            Icon(Icons.Outlined.Settings, "Location Settings")
        }
    }
}
