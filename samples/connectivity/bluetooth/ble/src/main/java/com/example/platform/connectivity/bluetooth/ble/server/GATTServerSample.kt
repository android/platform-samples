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

package com.example.platform.connectivity.bluetooth.ble.server

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.platform.connectivity.bluetooth.ble.BluetoothSampleBox
import com.google.android.catalog.framework.annotations.Sample


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
            Text(text = "Cannot run server:\nDevices does not support multi-advertisement")
        }
    }
}

@Composable
internal fun GATTServerScreen() {
    val context = LocalContext.current
    var enableServer by remember {
        mutableStateOf(GATTServerSampleService.isServerRunning.value)
    }
    var enableAdvertising by remember(enableServer) {
        mutableStateOf(enableServer)
    }
    val logs by GATTServerSampleService.serverLogsState.collectAsState()

    LaunchedEffect(enableServer, enableAdvertising) {
        val intent = Intent(context, GATTServerSampleService::class.java).apply {
            action = if (enableAdvertising) {
                GATTServerSampleService.ACTION_START_ADVERTISING
            } else {
                GATTServerSampleService.ACTION_STOP_ADVERTISING
            }
        }
        if (enableServer) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { enableServer = !enableServer }) {
                Text(text = if (enableServer) "Stop Server" else "Start Server")
            }

            Button(onClick = { enableAdvertising = !enableAdvertising }, enabled = enableServer) {
                Text(text = if (enableAdvertising) "Stop Advertising" else "Start Advertising")
            }
        }
        Text(text = logs)
    }
}
