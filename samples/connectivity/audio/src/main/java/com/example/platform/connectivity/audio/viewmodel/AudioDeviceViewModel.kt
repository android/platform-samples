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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.example.platform.connectivity.audio.datasource.PlatformAudioSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * View model for Active device and list of devices we can connect to
 */
class AudioDeviceViewModel constructor(
  private val platformAudioSource: PlatformAudioSource
): ViewModel() {

  /**
   * Get active audio device and pass to UI
   */
  val activeDeviceUiState: StateFlow<ActiveAudioDeviceUiState> =
    platformAudioSource.getActivePlatformAudioSourceStream.map { device ->
      val deviceUI = device?.toAudioDeviceUI(AudioDeviceState.Connected)
      if (deviceUI != null) {
        ActiveAudioDeviceUiState.OnActiveDevice(deviceUI)
      } else {
        ActiveAudioDeviceUiState.NotActive
      }
    }.stateIn(
      scope = viewModelScope,
      initialValue = ActiveAudioDeviceUiState.NotActive,
      //Keep flow subscribed for 5 seconds, helps with configuration changes
      started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
    )

  // Convert AudioDeviceInfo to a ViewModel, also removes the connected device so we list only the device we can connect to
  private var availableDevices: Flow<List<AudioDeviceUI>> =
    combine(platformAudioSource.getActivePlatformAudioSourceStream,
            platformAudioSource.getAudioDevicesStream) { activeDevice: AudioDeviceInfo?, devices: List<AudioDeviceInfo> ->
      devices.map { audioDeviceInfo ->
        when(audioDeviceInfo.id)
        {
          platformAudioSource.pendingDeviceId -> audioDeviceInfo.toAudioDeviceUI(AudioDeviceState.Connecting)
          activeDevice?.id ->  audioDeviceInfo.toAudioDeviceUI(AudioDeviceState.Connected)
          else -> audioDeviceInfo.toAudioDeviceUI(AudioDeviceState.Available)
        }
      }
    }.map { value ->
      value.filterIndexed { _, audioDeviceUI ->
        audioDeviceUI.audioDeviceState != AudioDeviceState.Connected
      }
    }

  val availableDeviceUiState: StateFlow<AudioDeviceListUiState> =
      availableDevices.map { device ->
        AudioDeviceListUiState.Success(device)
      }.stateIn(
        scope = viewModelScope,
        initialValue = AudioDeviceListUiState.Loading,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
      )

  private val _errorUiState = MutableStateFlow<String?>(null)
  val errorUiState: StateFlow<String?> = _errorUiState.asStateFlow()

  /**
   * Try and connect to audio device
   */
  fun setAudioDevice(audioDeviceInfo: AudioDeviceInfo) {
    viewModelScope.launch {
      var success: Boolean = platformAudioSource.setAudioSource(audioDeviceInfo)

      if (!success) {
        _errorUiState.update { "Error Connecting to Device" }
      }
    }
  }

  fun onErrorMessageShown() {
    _errorUiState.update { null }
  }

  sealed interface AudioDeviceListUiState {
    object Loading : AudioDeviceListUiState
    data class Success(val audioDevices: List<AudioDeviceUI>) : AudioDeviceListUiState
  }

  sealed interface ActiveAudioDeviceUiState {
    object NotActive : ActiveAudioDeviceUiState
    data class OnActiveDevice(val audioDevice: AudioDeviceUI) : ActiveAudioDeviceUiState
  }

}
