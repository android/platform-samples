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

package com.example.platform.connectivity.telecom.screen

import android.R
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.core.telecom.CallEndpointCompat
import com.example.platform.connectivity.telecom.R.drawable
import com.example.platform.connectivity.telecom.VoipViewModel

@Composable
fun IncallScreen(callViewModel: VoipViewModel) {
    val callEndPoints by callViewModel.availableAudioRoutes.collectAsState()
    val muteState by callViewModel.isMuted.collectAsState()
    val activeState by callViewModel.isActive.collectAsState()

    IncallBottomBar(
        callEndPoints,
        muteState,
        callViewModel::toggleMute,
        callViewModel::toggleHoldCall,
        { callViewModel.disconnectCall()},
        activeState,
        callViewModel::setEndpoint
    )
}

@Composable
fun IncallBottomBar(
    endPoints: List<CallEndpointCompat>,
    muteState: Boolean,
    onMuteChanged: (Boolean) -> Unit,
    onHoldCall: (Boolean) -> Unit,
    onHangUp: () -> Unit,
    activeState: Boolean,
    onAudioDeviceSelected: (CallEndpointCompat) -> Unit,
) {

    var audioDeviceWidgetState by remember { mutableStateOf(false) }

    BottomAppBar(
        actions = {
            ToggleButton(
               drawable.mic_off,
                drawable.mic_on,
                muteState,
                onMuteChanged,
            )
            Box {
                IconButton(onClick = { audioDeviceWidgetState = !audioDeviceWidgetState }) {
                    Icon(
                        painter = painterResource(id = drawable.speaker),
                        contentDescription = "Localized description",
                    )
                }
                DropdownMenu(
                    expanded = audioDeviceWidgetState,
                    onDismissRequest = { audioDeviceWidgetState = false },
                ) {
                    endPoints.forEach{
                        CallEndPointItem(
                            endPointUI = it,
                            onDeviceSelected = onAudioDeviceSelected,
                        )
                    }
                }
            }
            ToggleButton(
                R.drawable.ic_menu_call,
                R.drawable.stat_sys_phone_call_on_hold,
                activeState,
                onHoldCall,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onHangUp,
                containerColor = Color.Red,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            ) {
                Icon(
                    painter = painterResource(id = drawable.call_end),
                    "Localized description",
                )
            }
        },
    )
}


@Composable
fun ToggleButton(
    positiveResID: Int,
    negativeResID: Int,
    toggleState: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    FilledIconToggleButton(checked = toggleState, onCheckedChange = onCheckedChange) {
        if (toggleState) {
            Icon(painter = painterResource(id = positiveResID), "Switch On")
        } else {
            Icon(painter = painterResource(id = negativeResID), "Switch Off")
        }
    }
}

/**
 * Displays the audio device with Icon and Text
 */
@SuppressLint("NewApi")
@Composable
private fun CallEndPointItem(
    endPointUI: CallEndpointCompat,
    onDeviceSelected: (CallEndpointCompat) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(endPointUI.name.toString()) },
        onClick = { onDeviceSelected(endPointUI) },
        leadingIcon = {

            Icon(
                Icons.Outlined.Phone,
                contentDescription = null,
            )
        }
    )
}