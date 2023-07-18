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

package com.example.platform.connectivity.telecom.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.platform.connectivity.telecom.model.TelecomCall
import com.example.platform.connectivity.telecom.model.TelecomCallAction
import com.example.platform.connectivity.telecom.model.TelecomCallRepository

/**
 * A simple BroadcastReceiver that routes the call notification actions to the TelecomCallRepository
 */
@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallBroadcast : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Get the action or skip if none
        val action = intent.getTelecomCallAction() ?: return
        val repo = TelecomCallRepository.instance ?: TelecomCallRepository.create(context)
        val call = repo.currentCall.value

        if (call is TelecomCall.Registered) {
            // If the call is still registered perform action
            call.processAction(action)
        } else {
            // Otherwise probably something went wrong and the notification is wrong.
            TelecomCallNotificationManager(context).updateCallNotification(call)
        }
    }

    /**
     * Get the [TelecomCallAction] parcelable object from the intent bundle.
     */
    private fun Intent.getTelecomCallAction() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(
                TelecomCallNotificationManager.TELECOM_NOTIFICATION_ACTION,
                TelecomCallAction::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(TelecomCallNotificationManager.TELECOM_NOTIFICATION_ACTION)
        }
}
