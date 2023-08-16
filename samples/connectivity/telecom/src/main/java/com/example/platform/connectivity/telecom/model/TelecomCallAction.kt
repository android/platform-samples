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

package com.example.platform.connectivity.telecom.model

import android.os.ParcelUuid
import android.os.Parcelable
import android.telecom.DisconnectCause
import kotlinx.parcelize.Parcelize

/**
 * Simple interface to represent related call actions to communicate with the registered call scope
 * in the [TelecomCallRepository.registerCall]
 *
 * Note: we are using [Parcelize] to make the actions parcelable so they can be directly used in the
 * call notification.
 */
sealed interface TelecomCallAction : Parcelable {
    @Parcelize
    object Answer : TelecomCallAction

    @Parcelize
    data class Disconnect(val cause: DisconnectCause) : TelecomCallAction

    @Parcelize
    object Hold : TelecomCallAction

    @Parcelize
    object Activate : TelecomCallAction

    @Parcelize
    data class ToggleMute(val isMute: Boolean) : TelecomCallAction

    @Parcelize
    data class SwitchAudioEndpoint(val endpointId: ParcelUuid) : TelecomCallAction

    @Parcelize
    data class TransferCall(val endpointId: ParcelUuid) : TelecomCallAction
}
