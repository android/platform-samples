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

package com.example.platform.location.currentLocation

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.platform.location.permission.RationaleState
import com.example.platform.location.permission.ShowRationale
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.catalog.framework.annotations.Sample
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import java.time.Instant

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Sample(
    name = "Location - Getting Current Location",
    description = "This Sample demonstrate how to request of current location"
)
@Composable
fun CurrentLocationScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val fineLocationState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    var locationStr by remember {
        mutableStateOf("")
    }

    var showRationale by remember {
        mutableStateOf(false)
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
                "Please grant Location access , as it is required to detect the current location of device." + "\n\nWould you like to continue?"
            ) { accepted ->
                if (accepted) {
                    fineLocationState.launchMultiplePermissionRequest()
                }
                showRationale = false
            })
        }
        Button(onClick = {
            if (fineLocationState.permissions[0].status.isGranted || fineLocationState.permissions[1].status.isGranted) {
                // getting last known location is faster and minimizes battery usage
                // This information may be out of date.
                // Location may be null as previously no client has access location
                // or location turned of in device setting.
                // Please handle for null case as well as additional check can be added before using the method
                scope.launch {
                    locationClient.lastLocation.addOnCompleteListener {
                        if (it.result == null) {
                            locationStr =
                                "No last known location. Try fetching the current location first"
                        } else {
                            locationStr =
                                "Current location is \n" + "lat : ${it.result.latitude}\n" +
                                        "long : ${it.result.longitude}\n" + "fetched at ${Instant.now()}"
                        }
                    }
                }
            } else {
                if (fineLocationState.shouldShowRationale) {
                    showRationale = true
                } else {
                    fineLocationState.launchMultiplePermissionRequest()
                }
            }
        }) {
            Text("Get last known location")
        }

        Button(onClick = {
            if (fineLocationState.permissions[0].status.isGranted || fineLocationState.permissions[1].status.isGranted) {
                //To get more accurate or fresher device location use this method
                scope.launch {
                    locationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token
                    ).addOnCompleteListener {
                        it.result?.let { fetchedLocation ->
                            locationStr =
                                "Current location is \n" + "lat : ${fetchedLocation.latitude}\n" +
                                        "long : ${fetchedLocation.longitude}\n" + "fetched at ${Instant.now()}"
                        }
                    }
                }
            } else {
                if (fineLocationState.shouldShowRationale) {
                    showRationale = true
                } else {
                    fineLocationState.launchMultiplePermissionRequest()
                }
            }
        }) {
            Text(text = "Get current location")
        }
        Text(
            text = locationStr
        )

    }

}

