/*
 * Copyright 2025 The Android Open Source Project
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

package com.example.platform.ui.live_updates

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Composable
fun LiveUpdateSample() {
    val notificationManager =
        LocalContext.current.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    SnackbarNotificationManager.initialize(LocalContext.current.applicationContext, notificationManager)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            NotificationPermission()
            Spacer(modifier = Modifier.height(4.dp))
            NotificationPostPromotedPermission()
            Text(stringResource( R.string.live_update_summary_text))
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    onCheckout()
                    scope.launch {
                        snackbarHostState.showSnackbar("Order placed")
                    }
                },
            ) {
                Text("Checkout")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
fun onCheckout() {
    SnackbarNotificationManager.start()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission() {
    @SuppressLint("InlinedApi") // Granted at install time on API <33.
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    if (!notificationPermissionState.status.isGranted) {
        NotificationPermissionCard(
            shouldShowRationale = notificationPermissionState.status.shouldShowRationale,
            onGrantClick = {
                notificationPermissionState.launchPermissionRequest()
            },
            modifier = Modifier
                .fillMaxWidth(),
            permissionStringResourceId = R.string.permission_message,
            permissionRationalStringResourceId = R.string.permission_rationale,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@Composable
fun NotificationPostPromotedPermission() {
    val context = LocalContext.current
    var isPostPromotionsEnabled by remember { mutableStateOf(SnackbarNotificationManager.isPostPromotionsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isPostPromotionsEnabled = SnackbarNotificationManager.isPostPromotionsEnabled()
    }
    if (!isPostPromotionsEnabled) {
        Text(
            text = stringResource(R.string.post_promoted_permission_message),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            },
        ) {
            Text(text = stringResource(R.string.to_settings))
        }
    }
}

@Composable
private fun NotificationPermissionCard(
    shouldShowRationale: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    permissionStringResourceId: Int,
    permissionRationalStringResourceId: Int,
) {
    Card(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(permissionStringResourceId),
            modifier = Modifier.padding(16.dp),
        )
        if (shouldShowRationale) {
            Text(
                text = stringResource(permissionRationalStringResourceId),
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
           Button(onClick = onGrantClick) {
                Text(text = stringResource(R.string.permission_grant))
            }
        }
    }
}