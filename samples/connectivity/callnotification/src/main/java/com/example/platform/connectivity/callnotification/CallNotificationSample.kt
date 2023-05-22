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


package com.example.platform.connectivity.callnotification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Call Notification Sample",
    description = "Sample demonstrating how to make incoming call notifications and in call notifications",
)
class CallNotificationSample : ComponentActivity() {

    var notificationSource: NotificationSource<NotificationReceiver>? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationSource = NotificationSource(this, NotificationReceiver::class.java)

        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background,
                ) {
                    // We should be using make_own_call permissions but this requires
                    // implementation of the telecom API to work correctly.
                    // Please see telecom example for full implementation
                    val callPermission = rememberPermissionState(
                        Manifest.permission.POST_NOTIFICATIONS,
                    )

                    if (callPermission.status.isGranted) {
                        EntryPoint(notificationSource!!)
                    } else {
                        PermissionWidget(callPermission)
                    }
                }
            }
        }
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val notificationStateValue = it.getIntExtra(
                    NotificationSource.NOTIFICATION_ACTION,
                    NotificationSource.Companion.NotificationState.CANCEL.ordinal,
                )

                var message = "No User Input"

                when (notificationStateValue) {
                    NotificationSource.Companion.NotificationState.ANSWER.ordinal -> message =
                        "Answered"

                    NotificationSource.Companion.NotificationState.REJECT.ordinal -> message =
                        "Rejected"

                    else -> {
                        message =
                            "Cancelled"
                    }
                }

                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        notificationSource?.onCancelNotification()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionWidget(permissionsState: PermissionState) {
    var showRationale by remember(permissionsState) {
        mutableStateOf(false)
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = {
                Text(text = "")
            },
            text = {
                Text(text = "")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionsState.launchPermissionRequest()
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                    },
                ) {
                    Text("Dismiss")
                }
            },
        )
    }

    Button(
        onClick = {
            if (permissionsState.status.shouldShowRationale) {
                showRationale = true
            } else {
                permissionsState.launchPermissionRequest()
            }
        },
    ) {
        Text(text = "Grant Permission")
    }
}

@Composable
fun EntryPoint(
    notificationSource: NotificationSource<CallNotificationSample.NotificationReceiver>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Button(onClick = { notificationSource.postIncomingCall() }) {
            Text("Post Incoming Call")
        }
        Button(onClick = { notificationSource.postOnGoingCall() }) {
            Text("Post In Call")
        }
    }
}
