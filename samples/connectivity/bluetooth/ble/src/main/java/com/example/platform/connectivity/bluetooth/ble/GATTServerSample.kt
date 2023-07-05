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
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.catalog.framework.annotations.Sample
import java.util.UUID


@Sample(
    name = "Create a GATT server",
    description = "Shows how to create a GATT server and communicate with the GATT client",
    documentation = "https://developer.android.com/reference/android/bluetooth/BluetoothGattServer",
    tags = ["bluetooth"],
)
@Composable
fun GATTServerSample() {
    // In addition to the Bluetooth permissions we also need the BLUETOOTH_ADVERTISE from Android 12
    val extraPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setOf(Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        emptySet()
    }
    BluetoothSampleBox(extraPermissions = extraPermissions) { adapter ->
        if (adapter.isMultipleAdvertisementSupported) {
            GATTServerScreen()
        } else {
            Text(text = "Devices does not support multi-advertisement")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
internal fun GATTServerScreen() {
    // We will update the logs whenever something change in the server
    var logs by remember {
        mutableStateOf("Starting server")
    }
    // Keeps the instance of the created GATT server
    var server by remember {
        mutableStateOf<BluetoothGattServer?>(null)
    }

    // This effect will handle the creation of the server using the provided callbacks
    GATTServerEffect(
        serverCallback = object : BluetoothGattServerCallback() {

            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int,
            ) {
                logs += "\nConnection state change: ${newState.toConnectionStateString()}. New device: ${device.name} ${device.address}"
                // You should keep a list of connected device to manage them
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray,
            ) {
                logs += "\nCharacteristic Write request: $requestId\nData: ${String(value)} (offset $offset)"
                // Here you should apply the write of the characteristic and notify connected
                // devices that it changed

                // If response is needed reply to the device that the write was successful
                if (responseNeeded) {
                    server?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null,
                    )
                }
            }

            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                logs += "\nMTU change request: $mtu"
            }
        },
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                logs += "\nStarted advertising"
            }

            override fun onStartFailure(errorCode: Int) {
                logs += "\nFailed to start advertising: $errorCode"
            }
        },
        onServerOpened = {
            logs += "\nGATT server opened"
            server = it
        },
    )
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(text = logs)
    }
}

// Random UUID for our service known between the client and server to allow communication
internal val SERVICE_UUID = UUID.fromString("9f42ba3a-75ea-4ca1-92d0-aef57f0479e6")
// Same as the service but for the characteristic
internal val CHARACTERISTIC_UUID = UUID.fromString("5aade5a7-14ea-43f7-a136-16cb92cddf35")

@SuppressLint("MissingPermission")
@Composable
private fun GATTServerEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    serverCallback: BluetoothGattServerCallback,
    advertiseCallback: AdvertiseCallback,
    onServerOpened: (BluetoothGattServer) -> Unit,
) {
    val context = LocalContext.current
    val manager = context.getSystemService<BluetoothManager>()!!
    val bluetoothLeAdvertiser = manager.adapter.bluetoothLeAdvertiser
    val currentServerCallback by rememberUpdatedState(serverCallback)
    val currentAdvertiseCallback by rememberUpdatedState(advertiseCallback)
    val currentOnServerOpened by rememberUpdatedState(onServerOpened)

    // Create our service with a characteristic for our GATT server
    val service = remember {
        BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).also {
            it.addCharacteristic(
                BluetoothGattCharacteristic(
                    CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE,
                ),
            )
        }
    }

    // Keep track of the created server
    var gattServer by remember {
        mutableStateOf<BluetoothGattServer?>(null)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                gattServer = manager.openGattServer(context, currentServerCallback).also {
                    it.addService(service)
                    currentOnServerOpened(it)
                }

                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                    .build()

                val data = AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .build()

                bluetoothLeAdvertiser.startAdvertising(
                    settings,
                    data,
                    currentAdvertiseCallback,
                )
            } else if (event == Lifecycle.Event.ON_STOP) {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
                gattServer?.close()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and close the connection
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            gattServer?.close()
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
        }
    }
}
