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
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.M)
@Sample(
    name = "Find devices sample",
    description = "This example will demonstrate how to scanning for Low Energy Devices"
)
@Composable
fun FindDevicesSample() {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService<BluetoothManager>()

    if (bluetoothManager == null || bluetoothManager.adapter == null) {
        Text(text = "Sample not supported in this device. Missing the Bluetooth Manager")
    } else {
        FindBLEDevicesScreen(FindDeviceController(bluetoothManager.adapter))
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FindBLEDevicesScreen(
    findDeviceController: FindDeviceController,
) {
    val multiplePermissionsState =
        rememberMultiplePermissionsState(FindDeviceController.bluetoothPermissionSet)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (multiplePermissionsState.allPermissionsGranted) {
            ListOfDevicesWidget(findDeviceController)
        } else {
            PermissionWidget(multiplePermissionsState)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionWidget(permissionsState: MultiplePermissionsState) {
    var showRationale by remember(permissionsState) {
        mutableStateOf(false)
    }

    if (showRationale) {
        AlertDialog(onDismissRequest = { showRationale = false }, title = {
            Text(text = "")
        }, text = {
            Text(text = "")
        }, confirmButton = {
            TextButton(onClick = {
                permissionsState.launchMultiplePermissionRequest()
            }) {
                Text("Continue")
            }
        }, dismissButton = {
            TextButton(onClick = {
                showRationale = false
            }) {
                Text("Dismiss")
            }
        })
    }

    Button(onClick = {
        if (permissionsState.shouldShowRationale) {
            showRationale = true
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }) {
        Text(text = "Grant Permission")
    }
}


@RequiresApi(Build.VERSION_CODES.M)
@RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
@Composable
private fun ListOfDevicesWidget(findDeviceController: FindDeviceController) {
    val isScanning by findDeviceController.isScanning.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (isScanning) {
                findDeviceController.stopScan()
            } else {
                coroutineScope.launch {
                    findDeviceController.startScan(TimeUnit.SECONDS.toMillis(30))
                }
            }
        }) {
            Text(
                text = if (isScanning) {
                    "Stop Scanning"
                } else {
                    "Start Scanning"
                }
            )
        }
        ListOfBLEDevices(findDeviceController)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
private fun ListOfBLEDevices(findDeviceController: FindDeviceController) {
    val devices by findDeviceController.listOfDevices.collectAsState()

    LazyColumn(Modifier.padding(16.dp)) {
        if (devices.isEmpty()) {
            item {
                Text(text = "No devices found")
            }
        }
        items(devices) { item ->
            BluetoothItem(bluetoothDevice = item)
        }
    }

}

@SuppressLint("MissingPermission")
@Composable
private fun BluetoothItem(bluetoothDevice: BluetoothDevice) {
    Text(
        bluetoothDevice.name,
        modifier = Modifier
            .padding(8.dp, 0.dp)
    )
}
