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
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.widgets.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun ClaimCreditButton(
    context: Context,
    widgetId: Int,
    claimCreditIconBitmap: Bitmap? = null,
    isClaimVisible: RemoteInt? = null,
    petTaps: MutableRemoteInt? = null,
    resetValue: RemoteInt = 0.ri,
) {
    val bitmap = claimCreditIconBitmap ?: remember {
        WidgetAssetProvider.claimCreditBitmap(context)
    }

    val claimAction: () -> Unit = {
        val updated = BuddyPreferences.addCoinsForWidget(context, widgetId, 1)
        BuddyPreferences.saveTapsCount(context, widgetId, updated)
    }

    CompositionLocalProvider(
        RemoteWidgetActionBridge.getLocalWidget() provides
            RemoteWidgetActionBridge.createWidgetInformation(widgetId)
    ) {
        RemoteBox(
            contentAlignment = RemoteAlignment.BottomEnd,
            modifier = RemoteModifier.fillMaxSize(),
        ) {
            val baseModifier = if (isClaimVisible != null) {
                RemoteModifier
                    .padding(end = 68.rdp, bottom = 12.rdp)
                    .size(48.rdp)
                    .visibility(isClaimVisible)
            } else {
                RemoteModifier
                    .padding(end = 68.rdp, bottom = 12.rdp)
                    .size(48.rdp)
            }

            val buttonModifier = if (petTaps != null) {
                baseModifier.clickable(
                    combinedAction(
                        valueChange(petTaps, resetValue),
                        RemoteWidgetActionBridge.createLambdaAction(widgetId, claimAction),
                    )
                )
            } else {
                baseModifier.onClick(claimAction)
            }

            RemoteBox(modifier = buttonModifier) {
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                    drawScaledBitmap(
                        image = bitmap.asImageBitmap().rb,
                        contentDescription = "Claim Credit",
                    )
                }
            }
        }
    }
}

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun ClaimCreditButton(
    context: Context,
    widgetId: Int,
    isClaimVisible: RemoteInt,
    claimBitmap: Bitmap? = null,
): Unit = ClaimCreditButton(
    context = context,
    widgetId = widgetId,
    claimCreditIconBitmap = claimBitmap,
    isClaimVisible = isClaimVisible,
)
