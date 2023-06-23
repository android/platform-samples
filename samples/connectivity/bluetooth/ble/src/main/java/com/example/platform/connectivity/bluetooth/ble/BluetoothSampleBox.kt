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

package com.example.platform.connectivity.bluetooth.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.platform.base.PermissionBox


/**
 * This composable wraps the permission logic and checks if bluetooth it's available and enabled
 */
@Composable
fun BluetoothSampleBox(
    extraPermissions: Set<String> = emptySet(),
    content: @Composable BoxScope.(BluetoothAdapter) -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val bluetoothAdapter = context.getSystemService<BluetoothManager>()?.adapter

    // If we derive physical location from BT devices or if the device runs on Android 11 or below
    // we need location permissions otherwise we don't need to request them (see AndroidManifest).
    val locationPermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    } else {
        emptySet()
    }

    // For Android 12 and above we only need connect and scan
    val bluetoothPermissionSet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
    } else {
        setOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )
    }

    PermissionBox(
        permissions = (bluetoothPermissionSet + locationPermission + extraPermissions).toList(),
        contentAlignment = Alignment.Center,
    ) {
        // Check to see if the Bluetooth classic feature is available.
        val hasBT = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        // Check to see if the BLE feature is available.
        val hasBLE = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        // Check if the adapter is enabled
        var isBTEnabled by remember {
            mutableStateOf(bluetoothAdapter?.isEnabled == true)
        }

        when {
            bluetoothAdapter == null || !hasBT -> MissingFeatureText(text = "No bluetooth available")
            !hasBLE -> MissingFeatureText(text = "No bluetooth low energy available")
            !isBTEnabled -> BluetoothDisabledScreen { isBTEnabled = true }
            else -> content(bluetoothAdapter)
        }
    }
}

@Composable
fun BluetoothDisabledScreen(onEnabled: () -> Unit) {
    val result =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                onEnabled()
            }
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Bluetooth is disabled")
        Button(
            onClick = {
                result.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
        ) {
            Text(text = "Enable")
        }
    }
}

@Composable
private fun MissingFeatureText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error,
    )
}
