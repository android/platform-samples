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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
class FindDeviceController(private val bluetoothAdapter: BluetoothAdapter) {

    private var foundDevices = listOf<BluetoothDevice>()
    private var scanningJob: Job? = null

    //Scan settings control the latency and accuracy of results from the bluetooth stack.
    //Lower the latency the most power consumed.
    private var scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    var listOfDevices = MutableStateFlow(foundDevices)
    var isScanning = MutableStateFlow(false)

    companion object {

        private val locationPermission: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Manifest.permission.ACCESS_FINE_LOCATION
            } else {
                Manifest.permission.ACCESS_COARSE_LOCATION
            }

        val bluetoothPermissionSet: List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    locationPermission
                )
            } else {
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    locationPermission
                )
            }
    }

    /**
     * Checks is a scanning job is already running
     * Returns false if job is running
     * For devices lower then 33 Bluetooth Admin should be requested not bluetooth scan
     */
    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
    suspend fun startScan(scanPeriod: Long): Boolean {
        if (scanningJob?.isActive == true) {
            return false
        }
        foundDevices = emptyList()
        scan(scanPeriod)
        return true
    }

    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
    private suspend fun scan(scanPeriodMS: Long) = coroutineScope {
        scanningJob = launch(Dispatchers.Default) {
            isScanning.update { true }

            bluetoothAdapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            delay(scanPeriodMS)
            stopScan()
        }
    }

    // For devices lower then 33 Bluetooth Admin should be requested not bluetooth scan
    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
    fun stopScan() {
        scanningJob?.let {
            it.cancel()
            scanningJob = null
        }

        bluetoothAdapter.bluetoothLeScanner.stopScan(leScanCallback)
        isScanning.update { false }
    }

    /**
     * Called when bluetooth stack has found a device
     * Updates mutable list
     */
    private val leScanCallback: ScanCallback = object : ScanCallback() {

        //To read bluetooth properties bluetooth connect is required for API 31 and above
        //for devices lower only bluetooth permission is required
        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            //Lets only add devices when we have a name for it and lets one add it once to our list.
            if (foundDevices.find { it.address == result.device.address } == null) {
                result.device.name?.let {
                    foundDevices += (result.device)
                    listOfDevices.value = foundDevices
                }
            }
        }
    }
}