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
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.platform.base.PermissionBox
import com.google.android.catalog.framework.annotations.Sample
import com.google.android.catalog.framework.ui.theme.CatalogTheme

@Sample(
    name = "Call Notification Sample",
    description = "Sample demonstrating how to make incoming call notifications and in call notifications",
    documentation = "https://developer.android.com/reference/android/app/Notification.CallStyle",
)
class CallNotificationSample : ComponentActivity() {

    private lateinit var notificationSource: NotificationSource<NotificationReceiver>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationSource = NotificationSource(this, NotificationReceiver::class.java)

        setContent {
            CatalogTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize().statusBarsPadding(),
                    color = colorScheme.background,
                ) {
                    // We should be using make_own_call permissions but this requires
                    // implementation of the telecom API to work correctly.
                    // Please see telecom example for full implementation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionBox(permission = Manifest.permission.POST_NOTIFICATIONS) {
                            EntryPoint(notificationSource)
                        }
                    } else {
                        EntryPoint(notificationSource)
                    }
                }
            }
        }
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.let {
                val notificationStateValue = it.getIntExtra(
                    NotificationSource.NOTIFICATION_ACTION,
                    NotificationSource.Companion.NotificationState.CANCEL.ordinal,
                )

                val message = when (notificationStateValue) {
                    NotificationSource.Companion.NotificationState.ANSWER.ordinal -> "Answered"
                    NotificationSource.Companion.NotificationState.REJECT.ordinal -> "Rejected"
                    else -> "Cancelled"
                }

                NotificationSource.cancelNotification(context)

                //Using a toast message to keep example simple. We should be using a snackbar
                //https://developer.android.com/jetpack/compose/layouts/material#snackbar
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        NotificationSource.cancelNotification(this)
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
