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

package com.example.platform.connectivity.audio.datasource

import android.annotation.SuppressLint
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Manages Audio Device states and keeps alist of list we can connect to.
 * Keeps track of active audio device
 * Can switch to any audio device in the audio platform include bluetooth and LEA devices
 * Keeps the states of what we are trying connect too
 */
class PlatformAudioSource(
    private val audioManager: AudioManager
) {

    //Flow based on the active audio stream
    @SuppressLint("NewApi")
    val getActivePlatformAudioSourceStream: Flow<AudioDeviceInfo?> = callbackFlow {

        val listener =
            AudioManager.OnCommunicationDeviceChangedListener {
                onConnectingDeviceStateChangedListener.onStateChanged(DeviceState.Connected)
                trySend(it)
            }

        audioManager.addOnCommunicationDeviceChangedListener(
            Executors.newSingleThreadExecutor(),
            listener
        )

        trySend(audioManager.communicationDevice)

        awaitClose {
            audioManager.removeOnCommunicationDeviceChangedListener(listener)

            //Audio manager needs clearing in order to clean any bluetooth connections it may have.
            audioManager.clearCommunicationDevice()
        }
    }


    //This flow keeps a list of audio devices available in the platform
    @SuppressLint("NewApi")
    val getAudioDevicesStream: Flow<List<AudioDeviceInfo>> = callbackFlow {

        trySend(audioManager.availableCommunicationDevices)

        val audioDeviceCallback: AudioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                trySend(audioManager.availableCommunicationDevices)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                trySend(audioManager.availableCommunicationDevices)
            }
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        val onStateChangeListener = object : OnConnectingDeviceStateChangedListener {
            override fun onStateChanged(state: DeviceState) {

                if (state == DeviceState.Connected || state == DeviceState.Failed) {
                    pendingDeviceId = null
                    waitForDeviceCallback?.cancel()
                    setAudioSourceResponse.complete(state == DeviceState.Connected)
                }

                trySend(audioManager.availableCommunicationDevices)
            }
        }

        this@PlatformAudioSource.setOnConnectingDeviceStateChangedListener(onStateChangeListener)

        awaitClose {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        }
    }

    private var waitForDeviceCallback: Job? = null
    private var setAudioSourceResponse: CompletableDeferred<Boolean> = CompletableDeferred()
    private lateinit var onConnectingDeviceStateChangedListener: OnConnectingDeviceStateChangedListener
    var pendingDeviceId: Int? = null

    /**
     * Switches platform audio source
     * Bluetooth devices can take upto 30 seconds to connect
     */
    @SuppressLint("NewApi")
    suspend fun setAudioSource(audioDeviceInfo: AudioDeviceInfo): Boolean {
        setAudioSourceResponse = CompletableDeferred()
        pendingDeviceId = audioDeviceInfo.id

        onConnectingDeviceStateChangedListener.onStateChanged(DeviceState.Pending)

        waitForDeviceCallback = CoroutineScope(Dispatchers.Main).launch {

            if (!audioManager.setCommunicationDevice(audioDeviceInfo)) {
                onConnectingDeviceStateChangedListener.onStateChanged(DeviceState.Failed)
            }
            // wait a max of 30 seconds. Sometimes bluetooth devices can timeout
            delay(TimeUnit.SECONDS.toMillis(10))
            onConnectingDeviceStateChangedListener.onStateChanged(DeviceState.Failed)
        }

        return setAudioSourceResponse.await()
    }

    enum class DeviceState {
        Pending,
        Failed,
        Connected
    }

    /**
     * Updates listener with the state of the device we are trying to connect to
     * Pending, Failed or Connected will be return
     */
    private interface OnConnectingDeviceStateChangedListener {
        fun onStateChanged(state: DeviceState)
    }

    private fun setOnConnectingDeviceStateChangedListener(listener: OnConnectingDeviceStateChangedListener) {
        onConnectingDeviceStateChangedListener = listener
    }
}