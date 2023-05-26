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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.telecom.CallEndpointCompat
import com.example.platform.connectivity.telecom.TelecomManager
import com.example.platform.connectivity.telecom.VoipViewModel

class CallStatusUI {
    var caller: String? = null
    var isActive: Boolean = false
    var currentAudioDevice: String? = null
    var isMuted: Boolean? = false
    var callState: String? = null
    var audioDevices : List<CallEndpointCompat> = emptyList()
}

class EndPointUI(var isActive: Boolean, var callEndpoint: CallEndpointCompat)

@Preview
@Composable
fun CallStatusWidget() {
    val tempCaller = CallStatusUI()
    tempCaller.caller = "Luke Hopkins"
    tempCaller.isActive = false
    tempCaller.currentAudioDevice = "Speaker"
    tempCaller.isMuted = false
    tempCaller.callState = "Not in call"

   // CallStatusWidget(tempCaller)
}

@Composable
fun CallStatusWidget(callViewModel: VoipViewModel) {

    val callState by callViewModel.currentCallState.collectAsState()
    val callStatus = when(callState)
    {
        TelecomManager.CallState.INCALL ->  "In Call"
        TelecomManager.CallState.INCOMING ->  "Incoming Call"
        TelecomManager.CallState.OUTGOING ->  "Outgoing Call"
        else -> "No Call"
    }

    val activeDeviceName by callViewModel.activeAudioRoute.collectAsState()
    val isMuted by callViewModel.isMuted.collectAsState()
    val isActive by callViewModel.isActive.collectAsState()
    val activeDevices by callViewModel.availableAudioRoutes.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Column(Modifier.padding(30.dp)) {
            Text(
                text = String.format("Call Status: %s", callStatus),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = String.format("Audio Device: %s", activeDeviceName?.name),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = String.format("Mute State: %b", isMuted),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = String.format("Active State: %b", isActive),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = String.format("Caller: %s", callViewModel.CallerName),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
            )

            activeDevices.forEach{
                Text(
                    text = String.format("Caller: %s", it.name),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

