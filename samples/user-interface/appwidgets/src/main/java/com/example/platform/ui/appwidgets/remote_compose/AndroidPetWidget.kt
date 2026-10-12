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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
class AndroidPetWidget : RemoteComposeWidget() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                BuddyPreferences.ensureWidgetRegistered(context, id)
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
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
        val appContext = context.applicationContext ?: context
        val claimCreditIconBitmap = remember(appContext) {
            WidgetAssetProvider.claimCreditBitmap(appContext)
        }
        val storeIconBitmap = remember(appContext) {
            WidgetAssetProvider.storeBitmap(appContext)
        }
        val coins = BuddyPreferences.getCoinsForWidget(appContext, widgetId)

        val petTaps = rememberMutableRemoteInt(0)
        val nextPetTaps = petTaps + 1.ri
        val isClaimVisible = selectIfGe(petTaps, REQUIRED_TAPS_FOR_COIN.ri, 1.ri, 0.ri)

        RemoteBox(
            modifier = RemoteModifier.fillMaxSize().background(Color.White.rc),
            contentAlignment = RemoteAlignment.Center
        ) {
            // Materialize RemoteInt expression IDs in the active creation state before
            // action modifiers serialize them into nested action lists.
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
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
            )
        }
    }

    companion object {
        const val REQUIRED_TAPS_FOR_COIN = 3
    }
}
