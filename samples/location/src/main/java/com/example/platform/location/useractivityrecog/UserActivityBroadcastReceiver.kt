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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.platform.location.useractivityrecog.UserActivityTransitionManager.Companion.getActivityType
import com.example.platform.location.useractivityrecog.UserActivityTransitionManager.Companion.getTransitionType
import com.google.android.gms.location.ActivityTransitionResult

@Composable
fun UserActivityBroadcastReceiver(
    systemAction: String,
    systemEvent: (userActivity: String) -> Unit,
) {
    val context = LocalContext.current
    val currentSystemOnEvent by rememberUpdatedState(systemEvent)

    DisposableEffect(context, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val result = intent?.let { ActivityTransitionResult.extractResult(it) } ?: return
                var resultStr = ""
                for (event in result.transitionEvents) {
                    resultStr += "${getActivityType(event.activityType)} " +
                            "- ${getTransitionType(event.transitionType)}"
                }
                Log.d("UserActivityReceiver", "onReceive: $resultStr")
                currentSystemOnEvent(resultStr)
            }
        }
        context.registerReceiver(broadcast, intentFilter)
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}
