/*
 *
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.uwb.hellouwb.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.uwb.hellouwb.HelloUwbApplication
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException


private const val PERMISSION_REQUEST_CODE = 1234

class UwbRangingActivity : ComponentActivity() {

  private var userRequestedInstall = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestPermissions()
    (application as HelloUwbApplication).initContainer {
      runOnUiThread { setContent { HelloUwbApp((application as HelloUwbApplication).container) } }
    }

    /**
     * Check if device supports Ultra-wideband
     */
    val packageManager: PackageManager = applicationContext.packageManager
    val deviceSupportsUwb = packageManager.hasSystemFeature("android.hardware.uwb")

    if (!deviceSupportsUwb ) {
      Log.e("UWB Sample", "Device does not support Ultra-wideband")
      Toast.makeText(applicationContext, "Device does not support UWB", Toast.LENGTH_SHORT).show()
      //TODO: Uncomment this if you want to see it running on a non-supported device
      finishAndRemoveTask();
    }
    else {
      Toast.makeText(applicationContext, "Device supports UWB", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onResume() {
    super.onResume()
    if (
      checkCallingOrSelfPermission(Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    ) {
      ArCoreApk.getInstance().checkAvailabilityAsync(this) { availability ->
        if (availability.isSupported) {
          try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
              ArCoreApk.InstallStatus.INSTALLED -> {}
              ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                userRequestedInstall = false
              }
            }
          } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.e("UWB Sample", "User declined ARCore installation", e)
          } catch (e: Exception) {
            Log.e("UWB Sample", "ARCore installation check failed", e)
          }
        }
      }
    }
  }

  private fun requestPermissions() {
    val missingPermissions = getMissingPermissions()
    if (missingPermissions.isNotEmpty()) {
      requestPermissions(missingPermissions, PERMISSION_REQUEST_CODE)
    }
  }

  private fun getMissingPermissions(): Array<String> =
    PERMISSIONS_REQUIRED.filter {
      checkCallingOrSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != PERMISSION_REQUEST_CODE) return
    // Don't re-request here: denied (or non-requestable) permissions are returned immediately,
    // which would cause an endless request loop.
    val deniedPermissions =
      permissions.filterIndexed { index, _ ->
        grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED
      }
    if (deniedPermissions.isNotEmpty()) {
      Log.w("UWB Sample", "Permissions denied: $deniedPermissions")
      Toast.makeText(
        applicationContext,
        "UWB ranging requires all requested permissions",
        Toast.LENGTH_LONG,
      ).show()
    }
  }

  companion object {

    private val PERMISSIONS_REQUIRED_BEFORE_T =
      listOf(
        // Permissions needed by Nearby Connection
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,

        // permission required by UWB API
        Manifest.permission.UWB_RANGING,

        // permission required by ARCore for UWB sensor fusion
        Manifest.permission.CAMERA,
      )

    private val PERMISSIONS_REQUIRED_T =
      arrayOf(
        Manifest.permission.NEARBY_WIFI_DEVICES,
      )

    // Runtime permission required by Nearby Connections starting in Android 17
    private val PERMISSIONS_REQUIRED_CINNAMON_BUN =
      arrayOf(
        Manifest.permission.ACCESS_LOCAL_NETWORK,
      )

    private val PERMISSIONS_REQUIRED =
      PERMISSIONS_REQUIRED_BEFORE_T.toMutableList()
        .apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addAll(PERMISSIONS_REQUIRED_T)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            addAll(PERMISSIONS_REQUIRED_CINNAMON_BUN)
          }
        }
        .toTypedArray()
  }
}
