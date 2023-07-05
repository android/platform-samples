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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.PermissionChecker
import com.example.platform.connectivity.telecom.model.TelecomCall
import com.example.platform.connectivity.telecom.model.TelecomCallAction

@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallNotificationManager(private val context: Context) {
    internal companion object {
        const val TELECOM_NOTIFICATION_ID = 200
        const val TELECOM_NOTIFICATION_ACTION = "telecom_action"
        const val TELECOM_NOTIFICATION_CHANNEL_ID = "telecom_channel"

        private val ringToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    fun updateCallNotification(call: TelecomCall) {
        // If notifications are not granted, skip it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            return
        }

        // Ensure that the channel is created
        createNotificationChannel()

        // Update notification
        when (call) {
            TelecomCall.None, is TelecomCall.Unregistered -> cancelNotification()
            is TelecomCall.Registered -> updateNotification(call)
        }
    }

    private fun updateNotification(call: TelecomCall.Registered) {
        val caller = Person.Builder()
            .setName(call.callAttributes.displayName)
            .setUri(call.callAttributes.address.toString())
            .setImportant(true)
            .build()
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, TelecomCallSampleActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Define the call style based on the call state and set the right actions
        val callStyle = if (call.isIncoming() && !call.isActive) {
            NotificationCompat.CallStyle.forIncomingCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.REJECTED),
                    ),
                ),
                getPendingIntent(TelecomCallAction.Answer),
            )
        } else {
            NotificationCompat.CallStyle.forOngoingCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.LOCAL),
                    ),
                ),
            )
        }

        val builder = NotificationCompat.Builder(context, TELECOM_NOTIFICATION_CHANNEL_ID)
            .setContentText("test")
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setSmallIcon(R.drawable.ic_round_call_24)
            .setOngoing(true)
            .setStyle(callStyle)

        if (call.isIncoming()) {
            builder.setSound(ringToneUri)
        }

        // TODO figure out why custom actions are not working
        if (call.isOnHold) {
            builder.addAction(
                R.drawable.ic_phone_paused_24, "Resume",
                getPendingIntent(
                    TelecomCallAction.Activate,
                ),
            )
        }

        @Suppress("MissingPermission")
        notificationManager.notify(TELECOM_NOTIFICATION_ID, builder.build())
    }

    private fun cancelNotification() {
        notificationManager.cancel(TELECOM_NOTIFICATION_ID)
    }

    private fun getPendingIntent(action: TelecomCallAction): PendingIntent {
        val callIntent = Intent(context, TelecomCallBroadcast::class.java)
        callIntent.putExtra(
            TELECOM_NOTIFICATION_ACTION,
            action,
        )

        return PendingIntent.getBroadcast(
            context,
            callIntent.hashCode(),
            callIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val name = "Telecom Channel"
        val descriptionText = "Handles the notifications when receiving or doing a call"
        val channel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        ).setName(name).setDescription(descriptionText).build()
        notificationManager.createNotificationChannel(channel)
    }
}
