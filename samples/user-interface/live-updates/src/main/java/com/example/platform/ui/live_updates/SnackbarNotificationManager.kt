/*
 * Copyright 2025 The Android Open Source Project
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

package com.example.platform.ui.live_updates

import android.app.Notification
import android.app.Notification.ProgressStyle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.IconCompat

object SnackbarNotificationManager {
    private lateinit var notificationManager: NotificationManager
    private lateinit var appContext: Context
    const val CHANNEL_ID = "live_updates_channel_id"
    private const val CHANNEL_NAME = "live_updates_channel_name"
    private const val NOTIFICATION_ID = 1234


    @RequiresApi(Build.VERSION_CODES.O)
    fun initialize(context: Context, notifManager: NotificationManager) {
        notificationManager = notifManager
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_DEFAULT)
        appContext = context
        notificationManager.createNotificationChannel(channel)
    }

    private enum class OrderState(val delay: Long) {
        INITIALIZING(5000) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): Notification.Builder {
                return buildBaseNotification(appContext, INITIALIZING)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("You order is being placed")
                    .setContentText("Confirming with bakery...")
                    .setStyle(buildBaseProgressStyle(INITIALIZING).setProgressIndeterminate(true))
            }
        },
        FOOD_PREPARATION(9000) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): Notification.Builder {
                return buildBaseNotification(appContext, FOOD_PREPARATION)
                    .setContentTitle("Your order is being prepared")
                    .setContentText("Next step will be delivery")
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.cupcake
                        ).toIcon(appContext)
                    )
                    .setStyle(buildBaseProgressStyle(FOOD_PREPARATION).setProgress(25))
            }
        },
        FOOD_ENROUTE(13000) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): Notification.Builder {
                return buildBaseNotification(appContext, FOOD_ENROUTE)
                    .setContentTitle("Your order is on its way")
                    .setContentText("Enroute to destination")
                    .setStyle(
                        buildBaseProgressStyle(FOOD_ENROUTE)
                            .setProgressTrackerIcon(
                                IconCompat.createWithResource(
                                    appContext, R.drawable.shopping_bag
                                ).toIcon(appContext)
                            )
                            .setProgress(50)
                    )
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.cupcake
                        ).toIcon(appContext)
                    )
            }
        },
        FOOD_ARRIVING(18000) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): Notification.Builder {
                return buildBaseNotification(appContext, FOOD_ARRIVING)
                    .setContentTitle("Your order is arriving and has been dropped off")
                    .setContentText("Enjoy & don't forget to refrigerate any perishable items.")
                    .setStyle(
                        buildBaseProgressStyle(FOOD_ARRIVING)
                            .setProgressTrackerIcon(
                                IconCompat.createWithResource(
                                    appContext, R.drawable.delivery_truck
                                ).toIcon(appContext)
                            )
                            .setProgress(75)
                    )
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.cupcake
                        ).toIcon(appContext)
                    )
            }
        },
        ORDER_COMPLETE(21000) {
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            override fun buildNotification(): Notification.Builder {
                return buildBaseNotification(appContext, ORDER_COMPLETE)
                    .setContentTitle("Your order is complete.")
                    .setContentText("Thank you for using JetSnack for your snacking needs.")
                    .setStyle(
                        buildBaseProgressStyle(ORDER_COMPLETE)
                            .setProgressTrackerIcon(
                                IconCompat.createWithResource(
                                    appContext, R.drawable.check_circle
                                ).toIcon(appContext)
                            )
                            .setProgress(100)
                    )
                    .setLargeIcon(
                        IconCompat.createWithResource(
                            appContext, R.drawable.cupcake
                        ).toIcon(appContext)
                    )
            }
        };


        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        fun buildBaseProgressStyle(orderState: OrderState): ProgressStyle {
            val pointColor = Color.valueOf(236f, 183f, 255f, 1f).toArgb()
            val segmentColor = Color.valueOf(134f, 247f, 250f, 1f).toArgb()
            var progressStyle = ProgressStyle()
                .setProgressPoints(
                    listOf(
                        ProgressStyle.Point(25).setColor(pointColor),
                        ProgressStyle.Point(50).setColor(pointColor),
                        ProgressStyle.Point(75).setColor(pointColor),
                        ProgressStyle.Point(100).setColor(pointColor)
                    )
                ).setProgressSegments(
                    listOf(
                        ProgressStyle.Segment(25).setColor(segmentColor),
                        ProgressStyle.Segment(25).setColor(segmentColor),
                        ProgressStyle.Segment(25).setColor(segmentColor),
                        ProgressStyle.Segment(25).setColor(segmentColor)

                    )
                )
            when (orderState) {
                INITIALIZING -> {}
                FOOD_PREPARATION -> {}
                FOOD_ENROUTE -> progressStyle.setProgressPoints(
                    listOf(
                        ProgressStyle.Point(25).setColor(pointColor)
                    )
                )

                FOOD_ARRIVING -> progressStyle.setProgressPoints(
                    listOf(
                        ProgressStyle.Point(25).setColor(pointColor),
                        ProgressStyle.Point(50).setColor(pointColor)
                    )
                )

                ORDER_COMPLETE -> progressStyle.setProgressPoints(
                    listOf(
                        ProgressStyle.Point(25).setColor(pointColor),
                        ProgressStyle.Point(50).setColor(pointColor),
                        ProgressStyle.Point(75).setColor(pointColor)
                    )
                )
            }
            return progressStyle
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun buildBaseNotification(appContext: Context, orderState: OrderState): Notification.Builder {
            val promotedExtras = Bundle()
            promotedExtras.putBoolean("android.requestPromotedOngoing", true)
            val notificationBuilder = Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setColorized(true)
                .addExtras(promotedExtras)

            when (orderState) {
                INITIALIZING -> {}
                FOOD_PREPARATION -> {}
                FOOD_ENROUTE -> {}
                FOOD_ARRIVING ->
                    notificationBuilder
                        .addAction(
                            Notification.Action.Builder(null, "Got it", null).build()
                        )
                        .addAction(
                            Notification.Action.Builder(null, "Tip", null).build()
                        )
                ORDER_COMPLETE ->
                    notificationBuilder
                        .addAction(
                            Notification.Action.Builder(
                                null, "Rate delivery", null).build()
                        )
            }
            return notificationBuilder
        }

        abstract fun buildNotification(): Notification.Builder
    }

    fun start() {
        for (state in OrderState.entries) {
            val notification = state.buildNotification().build()
            Handler(Looper.getMainLooper()).postDelayed({
                notificationManager.notify(NOTIFICATION_ID, notification)
            }, state.delay)
        }
    }
}
