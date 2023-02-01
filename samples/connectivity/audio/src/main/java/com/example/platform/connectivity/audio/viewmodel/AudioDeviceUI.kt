/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.platform.connectivity.audio.viewmodel

import android.media.AudioDeviceInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.platform.connectivity.audio.R

enum class AudioDeviceState {
    Connected,
    Connecting,
    Available,
}

/**
 * Data class for showing AudioDevice in our UI.
 */
data class AudioDeviceUI(
    val friendlyName: String,
    val name: Int?,
    val resIconId: Int,
    var audioDeviceState: AudioDeviceState,
    val audioDeviceInfo: AudioDeviceInfo
)

/**
 * Convert AudioDeviceInfo into a ViewModel so we can display it.
 */
fun AudioDeviceInfo.toAudioDeviceUI(audioDeviceState: AudioDeviceState): AudioDeviceUI {
    return AudioDeviceUI(
        friendlyName = productName.toString(),
        name = getDeviceResourceName(type),
        resIconId = getDeviceIcon(type),
        audioDeviceState = audioDeviceState,
        audioDeviceInfo = this,
    )
}

/**
 * Convert type to a friendlier name
 * Product name will return the devices name for internal audio such as Speakers and earpiece
 * this is not user friendly
 */
fun getDeviceResourceName(type: Int): Int? {
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> R.string.phone
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.string.speaker
        AudioDeviceInfo.TYPE_HEARING_AID -> R.string.hearingaid
        else -> {
            null
        }
    }
}

/**
 * Returns an Icon based on the device type
 */
fun getDeviceIcon(type: Int): Int {
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> R.drawable.phone_icon
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> R.drawable.speaker
        AudioDeviceInfo.TYPE_BLE_HEADSET -> R.drawable.headphones
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> R.drawable.headphones
        else -> {
            R.drawable.phone_icon
        }
    }
}

@Composable
fun AudioDeviceUI.getDeviceName(): String {
    return if (name == null) {
        friendlyName
    } else
        stringResource(id = name)
}

/**
 * Get a color based on the connection status of the device
 */
@Composable
fun AudioDeviceUI.getStatusColor(): Color {
    return when (audioDeviceState) {
        AudioDeviceState.Connected -> MaterialTheme.colorScheme.primary
        AudioDeviceState.Connecting -> MaterialTheme.colorScheme.error
        AudioDeviceState.Available -> MaterialTheme.colorScheme.onSurface
    }
}
