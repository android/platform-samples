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

package com.example.platform.connectivity.telecom.call

import android.Manifest
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.PhonePaused
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.core.telecom.CallEndpointCompat
import com.example.platform.connectivity.telecom.model.TelecomCall
import com.example.platform.connectivity.telecom.model.TelecomCallAction
import com.example.platform.connectivity.telecom.model.TelecomCallRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

/**
 * This composable observes the current state of the call and updates the UI based on its attributes
 *
 * Note: this only contains UI logic. All the telecom related actions are in [TelecomCallRepository]
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun TelecomCallScreen(repository: TelecomCallRepository, onCallFinished: () -> Unit) {
    // Collect the current call state and update UI
    val call by repository.currentCall.collectAsState()

    // If doing an outgoing call, fake the other end picks it up (this is only for this sample).
    (call as? TelecomCall.Registered)?.run {
        if (!isIncoming() && !isActive) {
            LaunchedEffect(Unit) {
                delay(2000)
                processAction(TelecomCallAction.Activate)
            }
        }
    }

    if (call is TelecomCall.Unregistered) {
        LaunchedEffect(Unit) {
            onCallFinished()
        }
    }

    when (val newCall = call) {
        is TelecomCall.Unregistered, TelecomCall.None -> {
            // Show call ended when there is no active call
            NoCallScreen()
        }

        is TelecomCall.Registered -> {
            // Call screen only contains the logic to represent the values of the active call
            // and process user input by calling the processAction of the active call.
            CallScreen(
                name = newCall.callAttributes.displayName.toString(),
                info = newCall.callAttributes.address.toString(),
                incoming = newCall.isIncoming(),
                isActive = newCall.isActive,
                isOnHold = newCall.isOnHold,
                isMuted = newCall.isMuted,
                currentEndpoint = newCall.currentCallEndpoint,
                endpoints = newCall.availableCallEndpoints,
                onCallAction = newCall::processAction,
            )
        }
    }
}

@Composable
private fun NoCallScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Call ended", style = MaterialTheme.typography.titleLarge)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CallScreen(
    name: String,
    info: String,
    incoming: Boolean,
    isActive: Boolean,
    isOnHold: Boolean,
    isMuted: Boolean,
    currentEndpoint: CallEndpointCompat?,
    endpoints: List<CallEndpointCompat>,
    onCallAction: (TelecomCallAction) -> Unit,
) {
    var showTransferEndpoints by remember {
        mutableStateOf(false)
    }
    val transferEndpoints = remember(endpoints) {
        endpoints.filter { it.type == CallEndpointCompat.TYPE_STREAMING }
    }
    Column(
        Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallInfoCard(name, info, isActive)
        if (incoming && !isActive) {
            IncomingCallActions(onCallAction)
        } else {
            OngoingCallActions(
                isActive = isActive,
                isOnHold = isOnHold,
                isMuted = isMuted,
                currentEndpoint = currentEndpoint,
                endpoints = endpoints,
                onCallAction = onCallAction,
                onTransferCall = {
                    showTransferEndpoints = true
                },
            )
        }
    }

    // Show a picker when selecting to transfer a call
    AnimatedVisibility(visible = showTransferEndpoints) {
        TransferCallDialog(
            transferEndpoints = transferEndpoints,
            onDismissRequest = {
                showTransferEndpoints = false
            },
            onCallAction = {
                showTransferEndpoints = false
                onCallAction(it)
            },
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun OngoingCallActions(
    isActive: Boolean,
    isOnHold: Boolean,
    isMuted: Boolean,
    currentEndpoint: CallEndpointCompat?,
    endpoints: List<CallEndpointCompat>,
    onCallAction: (TelecomCallAction) -> Unit,
    onTransferCall: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .shadow(1.dp)
            .padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CallControls(
            isActive = isActive,
            isOnHold = isOnHold,
            isMuted = isMuted,
            endpointType = currentEndpoint?.type ?: CallEndpointCompat.TYPE_UNKNOWN,
            availableTypes = endpoints.map { it.type },
            onCallAction = onCallAction,
            onTransferCall = onTransferCall,
        )
        FloatingActionButton(
            onClick = {
                onCallAction(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(
                            DisconnectCause.LOCAL,
                        ),
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.error,
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                modifier = Modifier.rotate(90f),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
private fun IncomingCallActions(onCallAction: (TelecomCallAction) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FloatingActionButton(
            onClick = {
                onCallAction(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(
                            DisconnectCause.REJECTED,
                        ),
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.error,
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                modifier = Modifier.rotate(90f),
            )
        }
        FloatingActionButton(
            onClick = {
                onCallAction(
                    TelecomCallAction.Answer,
                )
            },
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(imageVector = Icons.Rounded.Call, contentDescription = null)
        }
    }
}

@Composable
private fun CallInfoCard(name: String, info: String, isActive: Boolean) {
    Column(
        Modifier
            .fillMaxSize(0.5f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(imageVector = Icons.Rounded.Person, contentDescription = null)
        Text(text = name, style = MaterialTheme.typography.titleMedium)
        Text(text = info, style = MaterialTheme.typography.bodyMedium)

        if (!isActive) {
            Text(text = "Connecting...", style = MaterialTheme.typography.titleSmall)
        } else {
            Text(text = "Connected", style = MaterialTheme.typography.titleSmall)
        }

    }
}

/**
 * Displays the call controls based on the current call attributes
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CallControls(
    isActive: Boolean,
    isOnHold: Boolean,
    isMuted: Boolean,
    endpointType: @CallEndpointCompat.Companion.EndpointType Int,
    availableTypes: List<@CallEndpointCompat.Companion.EndpointType Int>,
    onCallAction: (TelecomCallAction) -> Unit,
    onTransferCall: () -> Unit,
) {
    val isLocalCall = endpointType != CallEndpointCompat.TYPE_STREAMING
    val micPermission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    var showRational by remember(micPermission.status) {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (micPermission.status.isGranted) {
            IconToggleButton(
                checked = isMuted,
                onCheckedChange = {
                    onCallAction(TelecomCallAction.ToggleMute(it))
                },
            ) {
                if (isMuted) {
                    Icon(imageVector = Icons.Rounded.MicOff, contentDescription = "Mic on")
                } else {
                    Icon(imageVector = Icons.Rounded.Mic, contentDescription = "Mic off")
                }
            }
        } else {
            IconButton(
                onClick = {
                    if (micPermission.status.shouldShowRationale) {
                        showRational = true
                    } else {
                        micPermission.launchPermissionRequest()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.MicOff,
                    contentDescription = "Missing mic permission",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        IconToggleButton(
            checked = endpointType == CallEndpointCompat.TYPE_SPEAKER,
            enabled = isLocalCall,
            onCheckedChange = { selected ->
                val type = if (selected) {
                    CallEndpointCompat.TYPE_SPEAKER
                } else {
                    // Switch to either wired headset or earpiece
                    availableTypes.firstOrNull { it == CallEndpointCompat.TYPE_WIRED_HEADSET }
                        ?: CallEndpointCompat.TYPE_EARPIECE
                }
                onCallAction(TelecomCallAction.SwitchAudioType(type))
            },
        ) {
            Icon(imageVector = Icons.Rounded.VolumeUp, contentDescription = "Toggle speaker")
        }
        if (availableTypes.contains(CallEndpointCompat.TYPE_BLUETOOTH)) {
            IconToggleButton(
                checked = endpointType == CallEndpointCompat.TYPE_BLUETOOTH,
                enabled = isLocalCall,
                onCheckedChange = { selected ->
                    val type = if (selected) {
                        CallEndpointCompat.TYPE_BLUETOOTH
                    } else {
                        // Switch to the default endpoint (as defined in TelecomCallRepo)
                        availableTypes.firstOrNull { it == CallEndpointCompat.TYPE_WIRED_HEADSET }
                            ?: CallEndpointCompat.TYPE_EARPIECE
                    }
                    onCallAction(TelecomCallAction.SwitchAudioType(type))
                },
            ) {
                if (endpointType == CallEndpointCompat.TYPE_BLUETOOTH) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothAudio,
                        contentDescription = "Disable bluetooth",
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothDisabled,
                        contentDescription = "Enable bluetooth",
                    )
                }
            }
        }
        IconToggleButton(
            enabled = isActive,
            checked = isOnHold,
            onCheckedChange = {
                val action = if (it) {
                    TelecomCallAction.Hold
                } else {
                    TelecomCallAction.Activate
                }
                onCallAction(action)
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.PhonePaused,
                contentDescription = "Pause or resume call",
            )
        }

        if (availableTypes.contains(CallEndpointCompat.TYPE_STREAMING)) {
            IconToggleButton(
                enabled = isActive,
                checked = !isLocalCall,
                onCheckedChange = {
                    if (it) {
                        onTransferCall()
                    } else {
                        // Switch back to the default audio type
                        onCallAction(TelecomCallAction.SwitchAudioType(CallEndpointCompat.TYPE_UNKNOWN))
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneForwarded,
                    contentDescription = "Transfer call",
                )
            }
        }
    }

    // Show a rational dialog if user didn't accepted the permissions
    if (showRational) {
        RationalMicDialog(
            onResult = { request ->
                if (request) {
                    micPermission.launchPermissionRequest()
                }
                showRational = false
            },
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransferCallDialog(
    transferEndpoints: List<CallEndpointCompat>,
    onDismissRequest: () -> Unit,
    onCallAction: (TelecomCallAction) -> Unit,
) {
    AlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = "Where to transfer the call?")
                LazyColumn {
                    items(transferEndpoints) {
                        Text(
                            text = it.name.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCallAction(TelecomCallAction.TransferCall(it.identifier))
                                },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismissRequest) {
                        Text(text = "Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun RationalMicDialog(onResult: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResult(false) },
        confirmButton = {
            TextButton(onClick = { onResult(true) }) {
                Text(text = "Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult(false) }) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "Mic permission required")
        },
        text = {
            Text(text = "In order to speak in a call we need mic permission. Please press continue and grant the permission in the next dialog.")
        },
    )
}
