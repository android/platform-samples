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
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.selectIfGe
import androidx.compose.remote.creation.compose.widgets.RemoteComposeWidget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
class AndroidPetWidget : RemoteComposeWidget() {

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE &&
            intent.getBooleanExtra(EXTRA_ALREADY_UPDATED, false)
        ) {
            return
        }
        val preservedActions = RemoteWidgetActionBridge.snapshotLambdaActions()
        runCatching {
            super.onReceive(context, intent)
        }
        preservedActions.putAll(RemoteWidgetActionBridge.snapshotLambdaActions())
        RemoteWidgetActionBridge.restoreLambdaActions(preservedActions)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val preservedActions = RemoteWidgetActionBridge.snapshotLambdaActions()
        for (id in appWidgetIds) {
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                BuddyPreferences.ensureWidgetRegistered(context, id)
            }
            runCatching {
                super.onUpdate(context, appWidgetManager, intArrayOf(id))
            }
            preservedActions.putAll(RemoteWidgetActionBridge.snapshotLambdaActions())
        }
        RemoteWidgetActionBridge.restoreLambdaActions(preservedActions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id ->
            BuddyPreferences.removeWidgetState(context, id)
        }
    }

    @SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
    @RemoteComposable
    @Composable
    override fun Content(context: Context, widgetId: Int) {
        RemoteWidgetActionBridge.resetLambdaCounter()
        val appContext = context.applicationContext ?: context
        val claimCreditIconBitmap = remember(appContext) {
            WidgetAssetProvider.claimCreditBitmap(appContext)
        }
        val storeIconBitmap = remember(appContext) {
            WidgetAssetProvider.storeBitmap(appContext)
        }
        val coins = BuddyPreferences.getCoinsForWidget(appContext, widgetId)

        val zeroTaps = 0.ri
        val petTaps = rememberMutableRemoteInt(0)
        val nextPetTaps = petTaps + 1.ri
        val isClaimVisible = selectIfGe(petTaps, REQUIRED_TAPS_FOR_COIN.ri, 1.ri, 0.ri)

        CompositionLocalProvider(
            RemoteWidgetActionBridge.getLocalWidget() provides
                RemoteWidgetActionBridge.createWidgetInformation(widgetId)
        ) {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.White.rc),
                contentAlignment = RemoteAlignment.Center
            ) {
                // Materialize RemoteInt expression IDs in the active creation state before
                // action modifiers serialize them into nested action lists.
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                    zeroTaps.getIdForCreationState(remoteComposeCreationState)
                    petTaps.getIdForCreationState(remoteComposeCreationState)
                    nextPetTaps.getIdForCreationState(remoteComposeCreationState)
                    isClaimVisible.getIdForCreationState(remoteComposeCreationState)
                }

                // Pet interactive touch target (placed below WidgetActionBar so button clicks
                // on the action bar are not intercepted by the pet touch target).
                RemoteBox(
                    modifier = RemoteModifier
                        .fillMaxSize()
                        .onTouchDown(valueChange(petTaps, nextPetTaps)),
                    contentAlignment = RemoteAlignment.Center
                ) {
                    RemoteColumn(
                        horizontalAlignment = RemoteAlignment.CenterHorizontally
                    ) {
                        RemoteText(
                            text = "Android Pet".rs,
                            color = Color.Black.rc
                        )
                        RemoteText(
                            text = "Coins: $coins".rs,
                            color = Color.DarkGray.rc
                        )
                    }
                }

                WidgetActionBar(
                    context = appContext,
                    widgetId = widgetId,
                    claimCreditIconBitmap = claimCreditIconBitmap,
                    storeIconBitmap = storeIconBitmap,
                    isClaimVisible = isClaimVisible,
                    petTaps = petTaps,
                    resetValue = zeroTaps,
                )
            }
        }
    }

    companion object {
        const val REQUIRED_TAPS_FOR_COIN = 3
        private const val EXTRA_ALREADY_UPDATED =
            "com.example.platform.ui.appwidgets.remote_compose.EXTRA_ALREADY_UPDATED"

        fun decodeBitmapFromResource(context: Context, @DrawableRes resId: Int): Bitmap =
            WidgetAssetProvider.decodeBitmap(context, resId)

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            AndroidPetWidget().onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            val intent = Intent(context, AndroidPetWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                putExtra(EXTRA_ALREADY_UPDATED, true)
            }
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(context: Context, appWidgetId: Int) {
            val manager = AppWidgetManager.getInstance(context)
            updateAppWidget(context, manager, appWidgetId)
        }
    }
}

fun refreshAndroidPetWidget(context: Context, widgetId: Int) {
    AndroidPetWidget.updateAppWidget(context, widgetId)
}
