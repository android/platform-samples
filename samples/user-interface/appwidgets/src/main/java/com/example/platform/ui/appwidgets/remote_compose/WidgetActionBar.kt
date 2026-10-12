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
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.runtime.Composable

@SuppressLint("RestrictedApiAndroidX", "RestrictedApi")
@RemoteComposable
@Composable
fun WidgetActionBar(
    context: Context,
    widgetId: Int,
    claimCreditIconBitmap: Bitmap? = null,
    storeIconBitmap: Bitmap? = null,
    isClaimVisible: RemoteInt? = null,
    petTaps: MutableRemoteInt? = null,
    resetValue: RemoteInt = 0.ri,
) {
    ClaimCreditButton(
        context = context,
        widgetId = widgetId,
        claimCreditIconBitmap = claimCreditIconBitmap,
        isClaimVisible = isClaimVisible,
        petTaps = petTaps,
        resetValue = resetValue,
    )
    StoreButton(
        context = context,
        widgetId = widgetId,
        storeIconBitmap = storeIconBitmap,
    )
}
