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
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.AppOpsManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.platform.base.PermissionBox
import com.google.android.catalog.framework.annotations.Sample
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@Sample(
    name = "Data Access",
    description = "Demonstrates how to implement data access auditing for your app to identify " +
            "unexpected data access, even from third-party SDKs and libraries.",
    documentation = "https://developer.android.com/guide/topics/data/audit-access",
)

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun DataAccess() {
    val permissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    // Requires at least coarse permission
    PermissionBox(
        permissions = permissions,
        requiredPermissions = listOf(permissions.first()),
    ) {
        // Only proceed if we have fine location access, as it's needed by WifiManager
        DataAccessAuditContent()
    }
}

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.R)
@RequiresPermission(
    Manifest.permission.ACCESS_FINE_LOCATION,
)
@Composable
fun DataAccessAuditContent() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val wifiManager = LocalContext.current.getSystemService(WifiManager::class.java)

    var dataAccessUpdates by remember {
        mutableStateOf("")
    }

    DataAccessUpdateEffect() { result ->
        dataAccessUpdates = result
    }

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                // grab scan results from WifiManager creates a sync access
                scope.launch(Dispatchers.IO) {
                    wifiManager.scanResults
                }
            },
        ) {
            Text("Trigger Sync Access (Get Wifi Scan Results)")
        }
        Button(
            onClick = {
                // grab current location creates an async access
                scope.launch(Dispatchers.IO) {
                    locationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token,
                    ).await()
                }
            },
        ) {
            Text(text = "Trigger Async Access (Get current location)")
        }
        Button(
            onClick = {
                // noteOp triggers self access
                scope.launch(Dispatchers.IO) {
                    AppOpsManagerCompat.noteOp(
                        context,
                        AppOpsManager.OPSTR_FINE_LOCATION,
                        Process.myUid(),
                        context.packageName,
                    )
                }
            },
        ) {
            Text(text = "Trigger Self Access (NoteOp)")
        }
        Text(
            text = dataAccessUpdates,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun DataAccessUpdateEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onUpdate: (result: String) -> Unit,
) {
    val context = LocalContext.current
    val currentOnUpdate by rememberUpdatedState(newValue = onUpdate)

    DisposableEffect(lifecycleOwner) {
        val appOpsManager = context.getSystemService<AppOpsManager>()
        val dataAccessCallback = object : AppOpsManager.OnOpNotedCallback() {
            override fun onNoted(op: SyncNotedAppOp) {
                currentOnUpdate("Sync Private Data Accessed: ${op.op}")
            }

            override fun onSelfNoted(op: SyncNotedAppOp) {
                currentOnUpdate("Self Private Data Accessed: ${op.op}")
            }

            override fun onAsyncNoted(op: AsyncNotedAppOp) {
                currentOnUpdate("Async Private Data Accessed: ${op.op}")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_CREATE) {
                appOpsManager?.setOnOpNotedCallback(context.mainExecutor, dataAccessCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and reset callback
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appOpsManager?.setOnOpNotedCallback(null, null)
        }
    }
}
