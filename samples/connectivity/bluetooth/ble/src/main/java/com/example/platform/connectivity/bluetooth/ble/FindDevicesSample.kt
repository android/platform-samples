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
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

@Sample(
    name = "FindDevicesSample",
    description = "This example will demonstrate how to scanning for Low Energy Devices"
)
@Composable
fun FindDevicesSamples() {
    val context = LocalContext.current
    val bluetoothManager =
        context.getSystemService<BluetoothManager>()

    FindBLEDevicesScreen(FindDeviceController(bluetoothManager!!.adapter))
}

@SuppressLint("MissingPermission", "NewApi")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FindBLEDevicesScreen(
    findDeviceController: FindDeviceController,
) {
    val multiplePermissionsState =
        rememberMultiplePermissionsState(FindDeviceController.bluetoothPermissionSet)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (multiplePermissionsState.allPermissionsGranted) {
                ListOfDevicesWidget(findDeviceController)
            } else {
                PermissionWidget(multiplePermissionsState)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWidget(
    multiplePermissionsState: MultiplePermissionsState,
) {
    Button(onClick = {
        multiplePermissionsState.launchMultiplePermissionRequest()
    }) {
        Text(text = "Grant Permission")
    }
}


@SuppressLint("InlinedApi")
@RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
@Composable
fun ListOfDevicesWidget(findDeviceController: FindDeviceController) {
    val bluetoothScanningUiState by findDeviceController.isScanning.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (bluetoothScanningUiState) {
        Button(onClick = {
            findDeviceController.stopScan()
        }) {
            Text(text = "Stop Scanning")
        }
    } else {
        Button(onClick = {
            coroutineScope.launch {
                findDeviceController.startScan(30 * 1000L)
            }
        }) {
            Text(text = "Start Scanning")
        }
    }

    ListOfBLEDevices(findDeviceController)
}

@Composable
fun ListOfBLEDevices(findDeviceController: FindDeviceController) {
    val devices by findDeviceController.listOfDevices.collectAsState()

    LazyColumn {
        items(devices) { item ->
            BluetoothItem(bluetoothDevice = item)
        }
    }

}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothItem(bluetoothDevice: BluetoothDevice) {
    Text(bluetoothDevice.name,
        modifier = Modifier
            .padding(8.dp, 0.dp))
}