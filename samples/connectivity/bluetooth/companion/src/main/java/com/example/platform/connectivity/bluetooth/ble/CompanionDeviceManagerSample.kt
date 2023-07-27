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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.getSystemService
import com.example.platform.base.PermissionBox
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
            CDMScreen(deviceManager) {
                selectedDevice = it.device ?: adapter.getRemoteDevice(it.name)
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

data class AssociatedDevice(
    val id: Int,
    val address: String,
    val name: String,
    val device: BluetoothDevice?,
)

@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CDMScreen(
    deviceManager: CompanionDeviceManager,
    onConnect: (AssociatedDevice) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var associatedDevice by remember {
        // If we already associated the device no need to do it again.
        mutableStateOf(getAssociatedDevice(deviceManager))
    }
    var errorMessage by remember(associatedDevice) {
        mutableStateOf("")
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        when (it.resultCode) {
            CompanionDeviceManager.RESULT_OK -> {
                associatedDevice = it.data?.getAssociationResult()
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(targetState = associatedDevice, label = "") { target ->
            if (target != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "ID: ${target.id}")
                    Text(text = "MAC: ${target.address}")
                    Text(text = "Name: ${target.name}")
                    Button(
                        onClick = {
                            onConnect(target)
                        },
                    ) {
                        Text(text = "Connect")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    deviceManager.disassociate(target.id)
                                } else {
                                    @Suppress("DEPRECATION")
                                    deviceManager.disassociate(target.address)
                                }
                                associatedDevice = null
                            }
                        },
                    ) {
                        Text(text = "Disassociate")
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val intentSender = requestDeviceAssociation(deviceManager)
                                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                        },
                    ) {
                        Text(text = "Find & Associate device")
                    }
                    if (errorMessage.isNotBlank()) {
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun getAssociatedDevice(deviceManager: CompanionDeviceManager): AssociatedDevice? {
    val associatedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        deviceManager.myAssociations.lastOrNull()?.toAssociatedDevice()
    } else {
        // Before Android 34 we can only get the MAC. We could use the BT adapter to find the
        // device, but to use CDM we only need the MAC.
        @Suppress("DEPRECATION")
        deviceManager.associations.lastOrNull()?.run {
            AssociatedDevice(
                id = -1,
                address = this,
                name = "",
                device = null,
            )
        }
    }
    return associatedDevice
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Intent.getAssociationResult(): AssociatedDevice? {
    var result: AssociatedDevice? = null
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
            result = AssociatedDevice(
                id = scanResult.advertisingSid,
                address = scanResult.device.address ?: "N/A",
                name = scanResult.scanRecord?.deviceName ?: "N/A",
                device = scanResult.device,
            )
        }
    }
    return result
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun AssociationInfo.toAssociatedDevice() = AssociatedDevice(
    id = id,
    address = deviceMacAddress?.toString() ?: "N/A",
    name = displayName?.ifBlank { "N/A" }?.toString() ?: "N/A",
    device = if (Build.VERSION.SDK_INT >= 34) {
        associatedDevice?.bleDevice?.device
    } else {
        null
    },
)

@RequiresApi(Build.VERSION_CODES.O)
suspend fun requestDeviceAssociation(deviceManager: CompanionDeviceManager): IntentSender {
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
