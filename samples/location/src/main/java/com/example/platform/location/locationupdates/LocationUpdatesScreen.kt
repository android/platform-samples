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

package com.example.platform.location.locationupdates

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.catalog.framework.annotations.Sample
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.Instant


@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Sample(
    name = "Location - Updates", description = "This Sample demonstrate how to get location updates"
)
@Composable
fun LocationUpdatesScreen() {
    val context = LocalContext.current
    val locationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val fineLocationState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    var receiveLocationUpdates by remember {
        mutableStateOf(false)
    }

    var showRationale by remember {
        mutableStateOf(false)
    }
    var locationStr by remember {
        mutableStateOf("")
    }
    // Create a location request
    // for more details about creating location request and
    // to check whether location settings are appropriate.
    // for location request
    // see [https://developer.android.com/training/location/change-location-settings]
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3 * 1000
    ).build()

    val locationCallBack: LocationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (currentLocation in result.locations) {
                    locationStr = "[${Instant.now()}]\n" +
                            " @[lat : ${currentLocation.latitude}" +
                            " lng : ${currentLocation.longitude}]\n" +
                            locationStr
                }
            }
        }
    }
    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            Log.d("TAG", "LocationUpdatesScreen: removing updates")
            locationClient.removeLocationUpdates(locationCallBack)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showRationale) {
            ShowRationale(rationaleState = RationaleState(
                "Location Permission Access",
                "Please grant Location access , as it is required to get the" +
                        " location updates for the  device." +
                        "\n\nWould you like to continue?"
            ) {
                if (it) {
                    fineLocationState.launchMultiplePermissionRequest()
                }
                showRationale = false
            })
        }

        // Toggle to start and stop location updates
        // before asking for periodic location updates,
        // it's good practice to fetch the current location
        // or get the last known location
        Switch(checked = receiveLocationUpdates, onCheckedChange = {
            receiveLocationUpdates = it
            if (receiveLocationUpdates) {
                if (fineLocationState.allPermissionsGranted) {
                    locationClient.requestLocationUpdates(
                        locationRequest, locationCallBack, Looper.getMainLooper()
                    )
                } else {
                    if (fineLocationState.shouldShowRationale) {
                        showRationale = true
                    } else {
                        fineLocationState.launchMultiplePermissionRequest()
                    }
                }
            } else {
                locationClient.removeLocationUpdates(locationCallBack)
            }
        })
        Text(text = locationStr)

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
)
