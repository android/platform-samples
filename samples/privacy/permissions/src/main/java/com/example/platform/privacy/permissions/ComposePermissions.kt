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

package com.example.platform.privacy.permissions

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.platform.privacy.permissions.common.PermissionScreen
import com.example.platform.privacy.permissions.common.PermissionScreenState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.catalog.framework.annotations.Sample

@OptIn(ExperimentalPermissionsApi::class)
@Sample(
    name = "Permissions using Compose",
    description = "This sample showcases how to request permission using Accompanist in Compose",
    documentation = "https://google.github.io/accompanist/permissions/",
    tags = ["permissions"]
)
@Composable
fun ComposePermissions() {
    val context = LocalContext.current

    // Register and remember the permission state
    val callPermissionState = rememberPermissionState(android.Manifest.permission.CALL_PHONE)

    // Utility function to calculate the state based on the PermissionState
    fun getScreenState(state: PermissionState) = when (state.status) {
        is PermissionStatus.Denied -> PermissionScreenState(
            title = "Call a phone", buttonText = "Grant permission"
        )

        PermissionStatus.Granted -> PermissionScreenState(
            title = "You can now call!", buttonText = "Call"
        )
    }

    // Defines the PermissionScreen UI based on the permission state and user interactions
    var screenState by remember(callPermissionState.status) {
        mutableStateOf(getScreenState(callPermissionState))
    }

    PermissionScreen(
        state = screenState,
        onClick = {
            // Always request permissions in-context, provide a rationale if needed and check its status
            // before using an API that requires a permission.
            when (callPermissionState.status) {
                PermissionStatus.Granted -> {
                    Toast.makeText(context, "Faking a call...", Toast.LENGTH_SHORT).show()
                }

                is PermissionStatus.Denied -> {
                    if (callPermissionState.status.shouldShowRationale) {
                        // Update our UI based on the user interaction by showing a rationale
                        screenState = PermissionScreenState(
                            title = "Call a phone",
                            buttonText = "Grant permission",
                            rationale = "In order to perform the call you need to grant access by accepting the next permission dialog.\n\nWould you like to continue?"
                        )
                    } else {
                        // Directly launch the system permission dialog
                        callPermissionState.launchPermissionRequest()
                    }
                }
            }
        },
        onRationaleReply = { accepted ->
            if (accepted) {
                callPermissionState.launchPermissionRequest()
            }

            // Reset the state after user interaction
            screenState = getScreenState(callPermissionState)
        }
    )
}
