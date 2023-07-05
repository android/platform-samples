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
import android.app.KeyguardManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.DisconnectCause
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.telecom.CallEndpointCompat
import com.example.platform.base.PermissionBox
import com.example.platform.connectivity.telecom.model.TelecomCall
import com.example.platform.connectivity.telecom.model.TelecomCallAction
import com.example.platform.connectivity.telecom.model.TelecomCallRepository
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Sample(
    name = "Telecom Call Sample",
    description = "A sample showcasing how to handle calls with the Jetpack Telecom API",
)
@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallSampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MPB", "onCreate:")
        setupCallActivity()
        setContent {
            MaterialTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // We should be using make_own_call permissions but this requires
                    // implementation of the telecom API to work correctly.
                    // Please see telecom example for full implementation
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        listOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.MANAGE_OWN_CALLS,
                        )
                    } else {
                        listOf(Manifest.permission.RECORD_AUDIO)
                    }
                    val context = LocalContext.current
                    val repository = remember {
                        TelecomCallRepository.instance ?: TelecomCallRepository.create(context)
                    }
                    PermissionBox(permissions = permissions) {
                        CallerScreen(repository)
                    }
                }
            }
        }
    }

    /**
     * Enable the calling activity to be shown in the lockscreen and dismiss the keyguard to enable
     * users to answer without unblocking.
     */
    private fun setupCallActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
            )
        }

        val keyguardManager = getSystemService<KeyguardManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CallerScreen(repository: TelecomCallRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect the current call state and update UI
    val call by repository.currentCall.collectAsState()

    // If call goes unregistered inform user
    if (call is TelecomCall.Unregistered) {
        LaunchedEffect(true) {
            Toast.makeText(context, "Call disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    when (val newCall = call) {
        is TelecomCall.Unregistered, TelecomCall.None -> {
            // Show calling menu when there is no active call
            NoCallScreen(
                incomingCall = {
                    Toast.makeText(context, "Incoming call in 2 seconds", Toast.LENGTH_SHORT).show()
                    scope.launch(Dispatchers.IO) {
                        repository.registerCall(
                            displayName = "Alice",
                            address = Uri.parse(""),
                            isIncoming = true,
                        )
                    }
                },
                outgoingCall = {
                    scope.launch(Dispatchers.IO) {
                        repository.registerCall(
                            displayName = "Bob",
                            address = Uri.parse(""),
                            isIncoming = false,
                        )

                        // Faking that the other end is not picking it
                        delay(2000)

                        // The other end answered, activate the call
                        (repository.currentCall.value as? TelecomCall.Registered)?.processAction(
                            TelecomCallAction.Activate,
                        )
                    }
                },
            )
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
private fun NoCallScreen(incomingCall: () -> Unit, outgoingCall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "No active call", style = MaterialTheme.typography.titleLarge)
        Button(onClick = incomingCall) {
            Text(text = "Receive fake call")
        }
        Button(onClick = outgoingCall) {
            Text(text = "Make fake call")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                isActive,
                isOnHold,
                isMuted,
                currentEndpoint,
                endpoints,
                onCallAction,
                showTransferEndpoints,
            )
        }
    }

    // Show a picker when selecting to transfer a call
    AnimatedVisibility(visible = showTransferEndpoints) {
        TransferCallDialog(
            showTransferEndpoints = showTransferEndpoints,
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
    showTransferEndpoints: Boolean,
) {
    var showTransferEndpoints1 = showTransferEndpoints
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
            onTransferCall = { showTransferEndpoints1 = true },
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

@Composable
fun CallControls(
    isActive: Boolean,
    isOnHold: Boolean,
    isMuted: Boolean,
    endpointType: @CallEndpointCompat.Companion.EndpointType Int,
    availableTypes: List<@CallEndpointCompat.Companion.EndpointType Int>,
    onCallAction: (TelecomCallAction) -> Unit,
    onTransferCall: () -> Unit,
) {
    val isLocalCall = endpointType != CallEndpointCompat.TYPE_STREAMING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconToggleButton(
            checked = isMuted,
            onCheckedChange = {
                onCallAction(TelecomCallAction.Mute(it))
            },
        ) {
            if (isMuted) {
                Icon(imageVector = Icons.Rounded.MicOff, contentDescription = "Mic on")
            } else {
                Icon(imageVector = Icons.Rounded.Mic, contentDescription = "Mic off")
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
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransferCallDialog(
    showTransferEndpoints: Boolean,
    transferEndpoints: List<CallEndpointCompat>,
    onDismissRequest: () -> Unit,
    onCallAction: (TelecomCallAction) -> Unit,
) {
    var showTransferEndpoints1 = showTransferEndpoints
    AlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
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
                    OutlinedButton(onClick = { showTransferEndpoints1 = false }) {
                        Text(text = "Dismiss")
                    }
                }
            }
        }
    }
}
