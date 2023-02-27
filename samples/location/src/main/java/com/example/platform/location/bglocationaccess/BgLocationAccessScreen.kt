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

package com.example.platform.location.bglocationaccess

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.platform.location.permission.PermissionRationaleDialog
import com.example.platform.location.permission.PermissionRequestButton
import com.example.platform.location.permission.RationaleState
import com.example.platform.location.utils.isBgLocationAccessGranted
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.catalog.framework.annotations.Sample
import java.util.concurrent.TimeUnit


@SuppressLint("InlinedApi")
@OptIn(ExperimentalPermissionsApi::class)
@Sample(
    name = "Location - Background Location updates",
    description = "This Sample demonstrate how to access location and get location updates when app is in background"
)
@Composable
fun BgLocationAccessScreen() {
    val foregroundLocationState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val bgLocationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val context = LocalContext.current

    // Keeps track of the rationale dialog state, needed when the user requires further rationale
    var rationaleState by remember {
        mutableStateOf<RationaleState?>(null)
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
            // Show rationale dialog when needed
            rationaleState?.run { PermissionRationaleDialog(rationaleState = this) }

            PermissionRequestButton(
                isGranted = foregroundLocationState.allPermissionsGranted,
                title = "Precise( or approximate) location access"
            ) {
                if (foregroundLocationState.shouldShowRationale) {
                    rationaleState = RationaleState(
                        "Request Precise(or approximate) Location",
                        "In order to use this feature please grant access by accepting " +
                                "the location permission dialog."
                                + "\n\nWould you like to continue?"
                    ) { accepted ->
                        if (accepted) {
                            foregroundLocationState.launchMultiplePermissionRequest()
                        }
                        rationaleState = null
                    }
                } else {
                    foregroundLocationState.launchMultiplePermissionRequest()
                }
            }

            // Background location permission needed from Android Q,
            // before Android Q, granting Fine or Coarse location access
            // automatically grants Background
            // location access
            if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                PermissionRequestButton(
                    isGranted = bgLocationPermissionState.status.isGranted,
                    title = "Background location access"
                ) {
                    if (foregroundLocationState.permissions[0].status.isGranted ||
                        foregroundLocationState.permissions[1].status.isGranted
                    ) {
                        if (bgLocationPermissionState.status.shouldShowRationale) {
                            rationaleState = RationaleState(
                                "Request background location",
                                "In order to use this feature please grant access by accepting "
                                        + "the background location permission dialog."
                                        + "\n\nWould you like to continue?"
                            ) { accepted ->
                                if (accepted) {
                                    bgLocationPermissionState.launchPermissionRequest()
                                }
                            }
                        } else {
                            bgLocationPermissionState.launchPermissionRequest()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please grant either Approximate location access permission or Fine"
                                    + "location access permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }
            Button(enabled = isBgLocationAccessGranted(
                foregroundLocationState,
                bgLocationPermissionState
            ),
                onClick = {
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        BgLocationWorker.workName,
                        ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequestBuilder<BgLocationWorker>(
                            2,
                            TimeUnit.MINUTES
                        ).build()
                    )
                }) {
                Text(text = "Receive background location updates")
            }


            Button(onClick = {
                WorkManager.getInstance(context).cancelUniqueWork(BgLocationWorker.workName)
            }) {
                Text(text = "Stop background location updates")
            }
        }

        FloatingActionButton(modifier = Modifier.align(Alignment.BottomEnd),
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }) {
            Icon(Icons.Outlined.Settings, "App Settings")
        }
    }
}