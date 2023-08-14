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

package com.example.platform.connectivity.audio

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Remember the current AudioDeviceState that observes changes in the active and available audio
 * devices for communication and allows to switch between while the composable is active and the
 * activity is visible.
 *
 * @see AudioDeviceState
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
internal fun rememberAudioDeviceState(): AudioDeviceState {
    val context = LocalContext.current
    val audioManager = context.getSystemService<AudioManager>()!!
    val state = remember {
        AudioDeviceState(audioManager)
    }

    // Observe lifecycles and composable events and register or unregister audio observers
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {

        // Called when a new device is available or when it becomes unavailable
        // We only care about comms devices, thus we just update our list
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                super.onAudioDevicesAdded(addedDevices)
                state.availableDevices = audioManager.availableCommunicationDevices
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                super.onAudioDevicesRemoved(removedDevices)
                state.availableDevices = audioManager.availableCommunicationDevices
            }
        }

        // Called when the active communication device changes
        val changedListener = AudioManager.OnCommunicationDeviceChangedListener {
            state.activeDevice = it
        }

        // Removing the comms listeners throws an exception if it was not previously added
        // Keep track of the listening state to avoid it.
        var isListening = false

        // Clear all observers and the selected device
        fun clearObservers() {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
            if (isListening) {
                audioManager.removeOnCommunicationDeviceChangedListener(changedListener)
                isListening = false
            }
            state.clearSelectedDevice()
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                audioManager.registerAudioDeviceCallback(
                    deviceCallback,
                    Handler(Looper.myLooper()!!),
                )
                audioManager.addOnCommunicationDeviceChangedListener({ it.run() }, changedListener)
                isListening = true
            } else if (event == Lifecycle.Event.ON_STOP) {
                clearObservers()
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            clearObservers()
        }
    }

    return state
}

/**
 * Keeps the state of the active and available communication devices and allows to change
 * the active device.
 *
 * @see rememberAudioDeviceState
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class AudioDeviceState(private val audioManager: AudioManager) {
    var activeDevice by mutableStateOf(audioManager.communicationDevice)

    var availableDevices by mutableStateOf<List<AudioDeviceInfo>>(
        audioManager.availableCommunicationDevices,
    )

    /**
     * Select the given device and waits for it to be active. If an error occurs or the device
     * does not become active in 30s or less it return false.
     */
    suspend fun selectDevice(deviceInfo: AudioDeviceInfo): Boolean =
        // For certain Bluetooth devices it can take up to 30s to activate them
        withTimeout(TimeUnit.SECONDS.toMillis(30)) {
            // Suspend the coroutine and wait till the device becomes active or it's cancelled
            suspendCancellableCoroutine { continuation ->
                // Listen for the device becoming active and return true
                val listener = object : AudioManager.OnCommunicationDeviceChangedListener {
                    override fun onCommunicationDeviceChanged(it: AudioDeviceInfo?) {
                        if (it?.id == deviceInfo.id) {
                            audioManager.removeOnCommunicationDeviceChangedListener(this)
                            continuation.resume(true)
                        }
                    }
                }
                audioManager.addOnCommunicationDeviceChangedListener(
                    /* executor = */ { it.run() },
                    /* listener = */ listener,
                )
                if (!audioManager.setCommunicationDevice(deviceInfo)) {
                    audioManager.removeOnCommunicationDeviceChangedListener(listener)
                    continuation.resume(false)
                }
                continuation.invokeOnCancellation {
                    // if the coroutine is cancelled stop listening for changes
                    // this won't cancel the previous setter, the device might become active later
                    audioManager.removeOnCommunicationDeviceChangedListener(listener)
                }
            }
        }

    /**
     * Clear the selected device. This will force the system to fallback to the default device.
     *
     * You should call this when the app does no longer uses the audio.
     */
    fun clearSelectedDevice() {
        audioManager.clearCommunicationDevice()
    }
}
