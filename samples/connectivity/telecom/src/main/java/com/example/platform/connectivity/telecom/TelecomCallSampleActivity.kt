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
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import com.example.platform.base.PermissionBox
import com.example.platform.connectivity.telecom.model.TelecomCallRepository
import com.google.android.catalog.framework.annotations.Sample


@Sample(
    name = "Telecom Call Sample",
    description = "A sample showcasing how to handle calls with the Jetpack Telecom API",
    documentation = "https://developer.android.com/guide/topics/connectivity/telecom",
)
@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallSampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCallActivity()

        // The repo contains all the call logic and communication with the Telecom SDK.
        val repository =
            TelecomCallRepository.instance ?: TelecomCallRepository.create(applicationContext)

        setContent {
            MaterialTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // To record the audio for the call
                    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

                    // We should be using make_own_call permissions but this requires
                    // implementation of the telecom API to work correctly.
                    // Please see telecom example for full implementation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        permissions.add(Manifest.permission.MANAGE_OWN_CALLS)
                    }

                    // To show call notifications we need permissions since Android 13
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    PermissionBox(permissions = permissions) {
                        TelecomCallScreen(repository)
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
