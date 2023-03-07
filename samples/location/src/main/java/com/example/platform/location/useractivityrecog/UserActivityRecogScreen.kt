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

package com.example.platform.location.useractivityrecog

import android.Manifest
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.example.platform.location.permission.PermissionRequestCard
import com.example.platform.location.permission.RationaleState
import com.example.platform.location.permission.ShowRationale
import com.example.platform.location.useractivityrecog.UserActivityTransitionManager.Companion.CUSTOM_INTENT_USER_ACTION
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Sample(
    name = "Location - User Activity Recognition",
    description = "This Sample demonstrate detection of user activity like walking, driving "
)
@Composable
fun UserActivityRecogScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentUserActivity by remember {
        mutableStateOf("")
    }
    val activityPermissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACTIVITY_RECOGNITION
        } else {
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
        }
    )

    var showRationale by remember { mutableStateOf(false) }
    var rationaleState by remember {
        mutableStateOf(RationaleState())
    }
    val manager = UserActivityTransitionManager(context)

    // Calling deregister on dispose
    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            manager.deregisterActivityTransitions()
        }
    }

    UserActivityBroadcastReceiver(systemAction = CUSTOM_INTENT_USER_ACTION) { userActivity ->
        currentUserActivity = userActivity
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showRationale) {
            ShowRationale(rationaleState)
        }

        PermissionRequestCard(
            isGranted = activityPermissionState.status.isGranted,
            title = "Activity permission access"
        ) {
            if (activityPermissionState.status.shouldShowRationale) {
                showRationale = true
                rationaleState = RationaleState(
                    "Request activity transition location",
                    "In order to use this feature please grant access by accepting " +
                            "the activity recognition permission." +
                            "\n\nWould you like to continue?"
                ) { accepted ->
                    if (accepted) {
                        activityPermissionState.launchPermissionRequest()
                    }
                    showRationale = false
                }
            } else {
                activityPermissionState.launchPermissionRequest()
            }
        }

        if (activityPermissionState.status.isGranted) {
            Button(onClick = {
                scope.launch {
                    manager.registerActivityTransitions()
                }
            }) {
                Text(text = "Register for activity transition updates")
            }
            Button(onClick = {
                scope.launch {
                    manager.deregisterActivityTransitions()
                }
            }) {
                Text(text = "Deregister for activity transition updates")
            }
            Text(
                text = "CurrentActivity is = $currentUserActivity"
            )
        }
    }
}