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
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresPermission
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
import com.example.platform.base.PermissionBox
import com.example.platform.location.utils.CUSTOM_INTENT_USER_ACTION
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Sample(
    name = "Location - User Activity Recognition",
    description = "This Sample demonstrate detection of user activity like walking, driving, etc.",
    documentation = "https://developer.android.com/training/location/transitions",
)
@Composable
fun UserActivityRecognitionScreen() {
    val activityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACTIVITY_RECOGNITION
    } else {
        "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
    }

    PermissionBox(permissions = listOf(activityPermission)) {
        UserActivityRecognitionContent()
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(
    anyOf = [
        Manifest.permission.ACTIVITY_RECOGNITION,
        "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
    ],
)
@Composable
fun UserActivityRecognitionContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember {
        UserActivityTransitionManager(context)
    }
    var currentUserActivity by remember {
        mutableStateOf("Unknown")
    }

    // Calling deregister on dispose
    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            scope.launch(Dispatchers.IO) {
                manager.deregisterActivityTransitions()
            }
        }
    }

    // Register a local broadcast to receive activity transition updates
    UserActivityBroadcastReceiver(systemAction = CUSTOM_INTENT_USER_ACTION) { userActivity ->
        currentUserActivity = userActivity
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    manager.registerActivityTransitions()
                }

            },
        ) {
            Text(text = "Register for activity transition updates")
        }
        Button(
            onClick = {
                currentUserActivity = ""
                scope.launch(Dispatchers.IO) {
                    manager.deregisterActivityTransitions()
                }
            },
        ) {
            Text(text = "Deregister for activity transition updates")
        }
        if (currentUserActivity.isNotBlank()) {
            Text(
                text = "CurrentActivity is = $currentUserActivity",
            )
        }
    }
}
