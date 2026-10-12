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
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.widgets.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun StoreButton(
    context: Context,
    widgetId: Int,
    storeIconBitmap: Bitmap? = null,
    isStoreVisible: RemoteInt? = null,
) {
    val bitmap = storeIconBitmap ?: remember {
        WidgetAssetProvider.storeBitmap(context)
    }

    CompositionLocalProvider(
        RemoteWidgetActionBridge.getLocalWidget() provides
            RemoteWidgetActionBridge.createWidgetInformation(widgetId)
    ) {
        RemoteBox(
            contentAlignment = RemoteAlignment.BottomEnd,
            modifier = RemoteModifier.fillMaxSize(),
        ) {
            val buttonModifier = if (isStoreVisible != null) {
                RemoteModifier
                    .padding(12.rdp)
                    .size(48.rdp)
                    .visibility(isStoreVisible)
            } else {
                RemoteModifier
                    .padding(12.rdp)
                    .size(48.rdp)
            }

            RemoteBox(
                modifier = buttonModifier
                    .onClick {
                        launchStoreActivity(context, widgetId)
                    },
            ) {
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                    drawScaledBitmap(
                        image = bitmap.asImageBitmap().rb,
                        contentDescription = "Store",
                    )
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun StoreButton(
    isStoreVisible: RemoteInt,
    context: Context,
    widgetId: Int,
): Unit = StoreButton(
    context = context,
    widgetId = widgetId,
    isStoreVisible = isStoreVisible,
)

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun StoreButton(
    context: Context,
    widgetId: Int,
    isStoreVisible: RemoteInt,
    iconBitmap: Bitmap? = null,
): Unit = StoreButton(
    context = context,
    widgetId = widgetId,
    storeIconBitmap = iconBitmap,
    isStoreVisible = isStoreVisible,
)

internal fun launchStoreActivity(context: Context, widgetId: Int) {
    val intent = Intent(context, StoreActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        putExtra(StoreActivity.EXTRA_WIDGET_ID, widgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val options = ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= 36) {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
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
