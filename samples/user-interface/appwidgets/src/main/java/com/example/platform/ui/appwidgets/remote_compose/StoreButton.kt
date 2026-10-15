/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.platform.ui.appwidgets.remote_compose

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.widgets.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.platform.ui.appwidgets.R

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun StoreButton(
    context: Context,
    widgetId: Int,
) {
    val imageBitmap = remember(context) {
        ContextCompat.getDrawable(context, R.drawable.shop_button)!!.toBitmap().asImageBitmap()
    }

    RemoteImage(
        remoteBitmap = imageBitmap.rb,
        contentDescription = "Store".rs,
        modifier = RemoteModifier
            .padding(12.rdp)
            .size(48.rdp)
            .onClick {
                launchStoreActivity(context, widgetId)
            },
    )
}

internal fun launchStoreActivity(context: Context, widgetId: Int) {
    val intent = Intent(context, StoreActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        // FLAG_ACTIVITY_NEW_TASK is required when starting an Activity from a BroadcastReceiver
        // context (RemoteComposeWidget dispatches onClick lambdas from onReceive).
        // FLAG_ACTIVITY_CLEAR_TOP and FLAG_ACTIVITY_SINGLE_TOP reuse an existing StoreActivity
        // instance if one is already open and deliver the newly selected widgetId via onNewIntent.
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    // Starting in Android 14 (API 34), apps must explicitly opt in to allow background activity
    // starts when triggered from a PendingIntent broadcast (such as a widget click).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val options = ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                )
            } else {
                @Suppress("DEPRECATION")
                setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
        }
        context.startActivity(intent, options.toBundle())
    } else {
        context.startActivity(intent)
    }
}
