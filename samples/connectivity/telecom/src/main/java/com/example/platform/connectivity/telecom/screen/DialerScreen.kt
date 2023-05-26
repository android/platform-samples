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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.platform.connectivity.telecom.TelecomManager

@Composable
fun DialerScreen(callViewModel: TelecomManager) {

    DialerBottomBar({callViewModel.makeOutGoingCall() }, { callViewModel.fakeIncomingCall()})
}

@Composable
fun DialerBottomBar(
    onOutgoingCall: () -> Unit,
    onIncomingCall: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.sym_call_outgoing),
                    contentDescription = "Outgoing Call"
                )
            },
            label = { Text("Outgoing Call") },
            selected = false,
            onClick = onOutgoingCall
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.sym_call_incoming),
                    contentDescription = "Incoming Call"
                )
            },
            label = { Text("Incoming Call") },
            selected = false,
            onClick = onIncomingCall
        )
    }
}