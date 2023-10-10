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

package com.example.platform.connectivity.bluetooth.cdm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.platform.base.PermissionBox
import com.example.platform.connectivity.bluetooth.ble.ConnectDeviceScreen
import com.example.platform.connectivity.bluetooth.ble.server.GATTServerSampleService.Companion.SERVICE_UUID
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

@Sample(
    name = "Companion Device Manager Sample",
    description = "This samples shows how to use the CDM to pair and connect with BLE devices",
    documentation = "https://developer.android.com/guide/topics/connectivity/companion-device-pairing",
    tags = ["bluetooth"],
)
@SuppressLint("InlinedApi", "MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CompanionDeviceManagerSample() {
    val context = LocalContext.current
    val deviceManager = context.getSystemService<CompanionDeviceManager>()
    val adapter = context.getSystemService<BluetoothManager>()?.adapter
    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }
    if (deviceManager == null || adapter == null) {
        Text(text = "No Companion device manager found. The device does not support it.")
    } else {
        if (selectedDevice == null) {
            DevicesScreen(deviceManager) { device ->
                selectedDevice = (device.device ?: adapter.getRemoteDevice(device.address))
            }
        } else {
            PermissionBox(permission = Manifest.permission.BLUETOOTH_CONNECT) {
                ConnectDeviceScreen(device = selectedDevice!!) {
                    selectedDevice = null
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DevicesScreen(
    deviceManager: CompanionDeviceManager,
    onConnect: (AssociatedDeviceCompat) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var associatedDevices by remember {
        // If we already associated the device no need to do it again.
        mutableStateOf(deviceManager.getAssociatedDevices())
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LaunchedEffect(associatedDevices) {
            associatedDevices.forEach {
                deviceManager.startObservingDevicePresence(it.address)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScanForDevicesMenu(deviceManager) {
            associatedDevices = associatedDevices + it
        }
        AssociatedDevicesList(
            associatedDevices = associatedDevices,
            onConnect = onConnect,
            onDisassociate = {
                scope.launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        deviceManager.disassociate(it.id)
                    } else {
                        @Suppress("DEPRECATION")
                        deviceManager.disassociate(it.address)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        deviceManager.stopObservingDevicePresence(it.address)
                    }
                    associatedDevices = deviceManager.getAssociatedDevices()
                }
            },
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ScanForDevicesMenu(
    deviceManager: CompanionDeviceManager,
    onDeviceAssociated: (AssociatedDeviceCompat) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var errorMessage by remember {
        mutableStateOf("")
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        when (it.resultCode) {
            CompanionDeviceManager.RESULT_OK -> {
                it.data?.getAssociationResult()?.run {
                    onDeviceAssociated(this)
                }
            }

            CompanionDeviceManager.RESULT_CANCELED -> {
                errorMessage = "The request was canceled"
            }

            CompanionDeviceManager.RESULT_INTERNAL_ERROR -> {
                errorMessage = "Internal error happened"
            }

            CompanionDeviceManager.RESULT_DISCOVERY_TIMEOUT -> {
                errorMessage = "No device matching the given filter were found"
            }

            CompanionDeviceManager.RESULT_USER_REJECTED -> {
                errorMessage = "The user explicitly declined the request"
            }

            else -> {
                errorMessage = "Unknown error"
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = "Find & associate another device running the GATTServerSample",
            )
            Button(
                modifier = Modifier.weight(0.3f),
                onClick = {
                    scope.launch {
                        val intentSender = requestDeviceAssociation(deviceManager)
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                },
            ) {
                Text(text = "Start")
            }
        }
        if (errorMessage.isNotBlank()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssociatedDevicesList(
    associatedDevices: List<AssociatedDeviceCompat>,
    onConnect: (AssociatedDeviceCompat) -> Unit,
    onDisassociate: (AssociatedDeviceCompat) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        stickyHeader {
            Text(
                text = "Associated Devices:",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(associatedDevices) { device ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Text(text = "ID: ${device.id}")
                    Text(text = "MAC: ${device.address}")
                    Text(text = "Name: ${device.name}")
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(
                        onClick = { onConnect(device) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Connect")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDisassociate(device) },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(MaterialTheme.colorScheme.error),
                        ),
                    ) {
                        Text(text = "Disassociate", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Intent.getAssociationResult(): AssociatedDeviceCompat? {
    var result: AssociatedDeviceCompat? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        result = getParcelableExtra(
            CompanionDeviceManager.EXTRA_ASSOCIATION,
            AssociationInfo::class.java,
        )?.toAssociatedDevice()
    } else {
        // Below Android 33 the result returns either a BLE ScanResult, a
        // Classic BluetoothDevice or a Wifi ScanResult
        // In our case we are looking for our BLE GATT server so we can cast directly
        // to the BLE ScanResult
        @Suppress("DEPRECATION")
        val scanResult = getParcelableExtra<ScanResult>(CompanionDeviceManager.EXTRA_DEVICE)
        if (scanResult != null) {
            result = AssociatedDeviceCompat(
                id = scanResult.advertisingSid,
                address = scanResult.device.address ?: "N/A",
                name = scanResult.scanRecord?.deviceName ?: "N/A",
                device = scanResult.device,
            )
        }
    }
    return result
}

@RequiresApi(Build.VERSION_CODES.O)
private suspend fun requestDeviceAssociation(deviceManager: CompanionDeviceManager): IntentSender {
    // Match only Bluetooth devices whose service UUID matches this pattern.
    // For this demo we will match our GATTServerSample
    val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
    val deviceFilter = BluetoothLeDeviceFilter.Builder()
        .setScanFilter(scanFilter)
        .build()

    val pairingRequest: AssociationRequest = AssociationRequest.Builder()
        // Find only devices that match this request filter.
        .addDeviceFilter(deviceFilter)
        // Stop scanning as soon as one device matching the filter is found.
        .setSingleDevice(true)
        .build()

    val result = CompletableDeferred<IntentSender>()

    val callback = object : CompanionDeviceManager.Callback() {
        override fun onAssociationPending(intentSender: IntentSender) {
            result.complete(intentSender)
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onDeviceFound(intentSender: IntentSender) {
            result.complete(intentSender)
        }

        override fun onAssociationCreated(associationInfo: AssociationInfo) {
            // This callback was added in API 33 but the result is also send in the activity result.
            // For handling backwards compatibility we can just have all the logic there instead
        }

        override fun onFailure(errorMessage: CharSequence?) {
            result.completeExceptionally(IllegalStateException(errorMessage?.toString().orEmpty()))
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val executor = Executor { it.run() }
        deviceManager.associate(pairingRequest, executor, callback)
    } else {
        deviceManager.associate(pairingRequest, callback, null)
    }
    return result.await()
}
