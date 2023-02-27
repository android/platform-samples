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

package com.example.platform.location.utils

import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// utility functions for Location samples

/**
 * Utility function to check whether Google Play services enabled or not
 */
suspend fun isGooglePlayServicesAvailable(context: Context): Boolean =
    withContext(Dispatchers.Default) {
        when (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> true
            else -> false
        }
    }

/**
 * Utility function to check whether location is enabled or not
 */
fun isLocationOn(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
}

/**
 * Check status of location permission for background location access
 */
@OptIn(ExperimentalPermissionsApi::class)
fun isBgLocationAccessGranted(
    foregroundLocationState: MultiplePermissionsState,
    bgLocationPermissionState: PermissionState,
): Boolean {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            bgLocationPermissionState.status.isGranted) ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    && foregroundLocationState.allPermissionsGranted)
}