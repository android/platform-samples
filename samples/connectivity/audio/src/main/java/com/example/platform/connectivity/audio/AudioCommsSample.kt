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

package com.example.platform.connectivity.audio

import android.Manifest
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.platform.base.PermissionBox
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Sample(
    name = "Communication Audio Manager Sample",
    description = "This sample shows how to use audio manager to for Communication application that self-manage the call.",
    documentation = "https://developer.android.com/guide/topics/connectivity/ble-audio/audio-manager",
    tags = ["audio"],
)
@RequiresApi(Build.VERSION_CODES.S)
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
@Composable
fun AudioCommsSample() {
    // The record permission is only needed for looping the audio not for the AudioManager
    PermissionBox(permission = Manifest.permission.RECORD_AUDIO) {
        AudioCommsScreen()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun AudioCommsScreen() {
    val scope = rememberCoroutineScope()

    // Get the current state of the communication audio devices. Check [AudioDeviceState].
    val state = rememberAudioDeviceState()
    val availableDevices = remember(state.availableDevices, state.activeDevice) {
        // Remove the active device from our list
        state.availableDevices.filter { it.id != state.activeDevice?.id }
    }
    var audioIssue by remember {
        mutableStateOf("")
    }

    // Only for testing purposes: open an audio loop that takes the active audio device, records the
    // audio and loops it back with a small delay.
    LaunchedEffect(Unit) {
        try {
            AudioLoopSource.openAudioLoop()
        } catch (e: Exception) {
            audioIssue = e.message ?: "Unknown error with audio loop"
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
    ) {
        stickyHeader {
            ActiveDeviceItem(state.activeDevice) {
                scope.launch {
                    // On click switch back to default device
                    state.clearSelectedDevice()
                }
            }
        }

        if (audioIssue.isNotBlank()) {
            item {
                Text(
                    text = audioIssue,
                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer),
                    style = TextStyle(color = MaterialTheme.colorScheme.error),
                )
            }
        }

        items(items = availableDevices, key = { it.id }) {
            var isLoading by remember {
                mutableStateOf(false)
            }
            AudioDeviceItem(deviceInfo = it, isLoading = isLoading) {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    // On item selected, switch and wait to the new device
                    audioIssue = if (state.selectDevice(it)) {
                        ""
                    } else {
                        "Error while selecting device"
                    }
                    isLoading = false
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun ActiveDeviceItem(device: AudioDeviceInfo?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(text = "Active device:", modifier = Modifier.padding(8.dp))
        AnimatedContent(targetState = device, label = "Active device") {
            if (it == null) {
                Text(text = "None")
            } else {
                AudioDeviceItem(it) {
                    onClick()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun AudioDeviceItem(
    deviceInfo: AudioDeviceInfo,
    isLoading: Boolean = false,
    onItemSelected: (AudioDeviceInfo) -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable { onItemSelected(deviceInfo) },
        headlineContent = {
            Text(deviceInfo.productName.toString())
        },
        leadingContent = {
            Text(deviceInfo.id.toString(), style = MaterialTheme.typography.headlineMedium)
        },
        trailingContent = {
            if (isLoading) {
                CircularProgressIndicator()
            }
        },
        supportingContent = {
            Text(
                when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Phone"
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT SCO"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT A2DP"
                    AudioDeviceInfo.TYPE_HEARING_AID -> "Hearing aid"
                    else -> "Type: ${deviceInfo.type}"
                },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
