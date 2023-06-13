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


package com.example.platform.connectivity.telecom

import android.Manifest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.platform.connectivity.callnotification.NotificationSource
import com.example.platform.connectivity.telecom.screen.CallStatusWidget
import com.example.platform.connectivity.telecom.screen.DialerScreen
import com.example.platform.connectivity.telecom.screen.IncallScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Sample(
    name = "TelecomSample",
    description = "Example application showing incoming and outgoing calls using the telecom jetpack library",
)
class TelecomSample : ComponentActivity() {

    companion object {
        lateinit var callViewModel: VoipViewModel
    }

    lateinit var notificationSource: NotificationSource<NotificationReceiver>

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callViewModel = VoipViewModel(this)
        notificationSource = NotificationSource(this, NotificationReceiver::class.java)

                setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    // We should be using make_own_call permissions but this requires
                    // implementation of the telecom API to work correctly.
                    // Please see telecom example for full implementation
                    val multiplePermissionsState =
                        rememberMultiplePermissionsState(
                            listOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.MANAGE_OWN_CALLS,
                            ),
                        )

                    if (multiplePermissionsState.allPermissionsGranted) {
                        callViewModel = VoipViewModel(this)
                        EntryPoint(callViewModel)
                    } else {
                        PermissionWidget(multiplePermissionsState)
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            callViewModel.currentCallState.collect {
                when (it) {
                    TelecomManager.CallState.INCOMING -> notificationSource.postIncomingCall()
                    TelecomManager.CallState.INCALL -> notificationSource.postOnGoingCall()
                    else -> {
                        notificationSource.cancelNotification()
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

                when (notificationStateValue) {
                    NotificationSource.Companion.NotificationState.ANSWER.ordinal -> callViewModel.answerCall()
                    NotificationSource.Companion.NotificationState.REJECT.ordinal -> callViewModel.rejectCall()
                    else -> callViewModel.disconnectCall()
                }
            }
        }

    }
}


@Composable
fun EntryPoint(callViewModel: VoipViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        CallingStatus(callViewModel)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            CallingBottomBar(callViewModel)
        }
    }
}

@Composable
fun CallingStatus(callViewModel: VoipViewModel) {

    CallStatusWidget(callViewModel)
}

@Composable
fun CallingBottomBar(callViewModel: VoipViewModel) {

    val callScreenState by callViewModel.currentCallState.collectAsState()

    when (callScreenState) {
        TelecomManager.CallState.INCALL -> {
            IncallScreen(callViewModel)
        }

        TelecomManager.CallState.INCOMING -> {

        }

        TelecomManager.CallState.OUTGOING -> {
            OutgoingCall()
        }

        else -> {
            DialerScreen(callViewModel)
        }
    }
}


@Composable
fun OutgoingCall() {
    Text(
        text = "Dialing out...",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.headlineMedium,
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionWidget(permissionsState: MultiplePermissionsState) {
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
                        permissionsState.launchMultiplePermissionRequest()
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
            if (permissionsState.shouldShowRationale) {
                showRationale = true
            } else {
                permissionsState.launchMultiplePermissionRequest()
            }
        },
    ) {
        Text(text = "Grant Permission")
    }
}