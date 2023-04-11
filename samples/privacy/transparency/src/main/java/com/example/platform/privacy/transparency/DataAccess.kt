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

package com.example.platform.privacy.transparency

import android.Manifest
import android.app.AppOpsManager
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.AppOpsManagerCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.catalog.framework.annotations.Sample
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Sample(
    name = "Data Access",
    description = "Demonstrates how to implement data access auditing for your app to identify " +
            "unexpected data access, even from third-party SDKs and libraries."
)

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun DataAccess() {
    val context = LocalContext.current
    val appOpsManager =
        context.getSystemService<AppOpsManager>(AppOpsManager::class.java) as AppOpsManager
    try {
        // you could add this to your app's onCreate() function to avoid re-registering the callback.
        // for our sample, we are going to catch the exception if the callback is already registered.
        appOpsManager.setOnOpNotedCallback(context.mainExecutor, DataAccessAuditListener)
    }
    catch (e: java.lang.IllegalStateException)
    {
        Log.d("DataAccess", "OpNotedCallback already registered")
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    if (permissionsState.permissions.all { it.status.isGranted }) {
        // Only proceed if we have fine location access, as it's needed by WifiManager
        DataAccessAuditContent()
    } else {
        LocationPermissions(
            text = "Location Permission (Precise Needed)",
            rationale = "In order to use this feature please grant access by accepting " +
                    "precise location permission." +
                    "\n\nWould you like to continue?",
            locationState = permissionsState
        )
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@RequiresPermission(
    Manifest.permission.ACCESS_FINE_LOCATION
)
@Composable
fun DataAccessAuditContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val wifiManager = LocalContext.current.getSystemService(WifiManager::class.java)

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please refer to the logcat for data access debug logs after " +
                    "selecting an option below."
        )
        Button(onClick = {
            // grab scan results from WifiManager creates a sync access
            scope.launch(Dispatchers.IO) {
                wifiManager.scanResults
            }
        }) {
            Text("Trigger Sync Access (Get Wifi Scan Results)")
        }
        Button(onClick = {
            // grab current location creates an async access
            scope.launch(Dispatchers.IO) {
                locationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }
        }) {
            Text(text = "Trigger Async Access (Get current location)")
        }
        Button(onClick = {
            // grab current location creates an async access
            scope.launch(Dispatchers.IO) {
                AppOpsManagerCompat.noteOp(
                    context,
                    AppOpsManager.OPSTR_FINE_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            }
        }) {
            Text(text = "Trigger Self Access (NoteOp)")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
object DataAccessAuditListener : AppOpsManager.OnOpNotedCallback() {
    // note: we are just logging to console for this sample but you can also integrate
    // other logging and reporting systems here to track your app's private data access.
    override fun onNoted(op: SyncNotedAppOp) {
        Log.d("DataAccessAuditListener","Sync Private Data Accessed: ${op.op}")
    }

    override fun onSelfNoted(op: SyncNotedAppOp) {
        Log.d("DataAccessAuditListener","Self Private Data accessed: ${op.op}")
    }

    override fun onAsyncNoted(op: AsyncNotedAppOp) {
        Log.d("DataAccessAuditListener", "Async Private Data Accessed: ${op.op}")
    }
}
