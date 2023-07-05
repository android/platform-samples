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

import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlCallback
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import com.example.platform.connectivity.audio.datasource.AudioLoopSource
import com.example.platform.connectivity.telecom.TelecomCallNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallRepository(
    private val applicationScope: CoroutineScope,
    private val callsManager: CallsManager,
    private val audioLoopSource: AudioLoopSource,
    private val notificationManager: TelecomCallNotificationManager,
) {

    companion object {
        var instance: TelecomCallRepository? = null
            private set

        /**
         * This does not illustrate best practices for instantiating classes in Android but for
         * simplicity we use this create method to create a singleton with the CallsManager class.
         */
        fun create(context: Context): TelecomCallRepository {
            Log.d("MPB", "New instance")
            check(instance == null) {
                "CallRepository instance already created"
            }

            // Create the Jetpack Telecom entry point
            val callsManager = CallsManager(context).apply {
                // Register with the telecom interface with the supported capabilities
                registerAppWithTelecom(
                    capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                            CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
                )
            }

            return TelecomCallRepository(
                applicationScope = CoroutineScope(SupervisorJob()),
                callsManager = callsManager,
                audioLoopSource = AudioLoopSource(),
                notificationManager = TelecomCallNotificationManager(context),
            ).also {
                instance = it
            }
        }
    }

    // Keeps track of the current TelecomCall state
    private val _currentCall: MutableStateFlow<TelecomCall> = MutableStateFlow(TelecomCall.None)
    val currentCall = _currentCall.asStateFlow()

    /**
     * Register a new call with the provided attributes.
     * Use the [currentCall] StateFlow to receive status updates and process call related actions.
     */
    fun registerCall(displayName: String, address: Uri, isIncoming: Boolean) {
        // For simplicity we don't support multiple calls
        check(_currentCall.value !is TelecomCall.Registered) {
            "There cannot be more than one call at the same time."
        }

        // Create the call attributes
        val attributes = CallAttributesCompat(
            displayName = displayName,
            address = address,
            direction = if (isIncoming) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            },
            callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            callCapabilities = (CallAttributesCompat.SUPPORTS_SET_INACTIVE
                    or CallAttributesCompat.SUPPORTS_STREAM
                    or CallAttributesCompat.SUPPORTS_TRANSFER),
        )

        // Creates a channel to send actions to the call scope.
        val actionSource = Channel<TelecomCallAction>()

        // We launch the call in our application scope so it keeps registered while the app is alive
        // or until the user explicitly disconnects it.
        applicationScope.launch {
            if (isIncoming) {
                // Fake incoming call delay
                delay(2000)
            }
            // Register the call and handle actions in the scope
            callsManager.addCall(attributes) {
                // Register the callback to be notified about other call actions
                // from other services or devices
                // TODO this should eventually be moved inside the addCall method b/290562928
                setCallback(
                    object : CallControlCallback {
                        override suspend fun onAnswer(callType: Int): Boolean {
                            TODO("Not yet implemented")
                        }

                        override suspend fun onDisconnect(disconnectCause: DisconnectCause): Boolean {
                            TODO("Not yet implemented")
                        }

                        override suspend fun onSetActive(): Boolean {
                            TODO("Not yet implemented")
                        }

                        override suspend fun onSetInactive(): Boolean {
                            TODO("Not yet implemented")
                        }
                    },
                )

                launch {
                    processCallStatus()
                }

                launch {
                    processCallActions(actionSource.consumeAsFlow())
                }

                // TODO Use the state value once b/290538853 is fixed
                _currentCall.value = TelecomCall.Registered(
                    id = getCallId(),
                    isActive = false,
                    isOnHold = false,
                    callAttributes = attributes,
                    isMuted = false,
                    currentCallEndpoint = null,
                    availableCallEndpoints = emptyList(),
                    actionSource = actionSource,
                )

                launch {
                    currentCallEndpoint.collect {
                        updateCurrentCall {
                            copy(currentCallEndpoint = it)
                        }
                    }
                }
                launch {
                    availableEndpoints.collect {
                        updateCurrentCall {
                            copy(availableCallEndpoints = it)
                        }
                    }
                }
                launch {
                    isMuted.collect {
                        updateCurrentCall {
                            copy(isMuted = it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Collect the action source to handle client actions inside the call scope
     */
    private suspend fun CallControlScope.processCallActions(actionSource: Flow<TelecomCallAction>) {
        try {
            actionSource.collect { action ->
                when (action) {
                    is TelecomCallAction.Answer -> doAnswer()

                    is TelecomCallAction.Disconnect -> {
                        doDisconnect(action)
                    }

                    is TelecomCallAction.SwitchAudioType -> doSwitchEndpoint(action)

                    is TelecomCallAction.TransferCall -> {
                        val call = _currentCall.value as? TelecomCall.Registered
                        val endpoints = call?.availableCallEndpoints?.firstOrNull {
                            it.identifier == action.id
                        }
                        requestEndpointChange(
                            endpoint = endpoints ?: return@collect,
                        )
                    }

                    TelecomCallAction.Hold -> if (setInactive()) {
                        updateCurrentCall {
                            copy(isOnHold = true)
                        }
                    }

                    TelecomCallAction.Activate -> if (setActive()) {
                        updateCurrentCall {
                            copy(
                                isActive = true,
                                isOnHold = false,
                            )
                        }
                    }

                    is TelecomCallAction.Mute -> {
                        // We cannot programmatically mute the telecom stack. Instead we just update
                        // the state of the call and this will start/stop audio capturing.
                        updateCurrentCall {
                            copy(isMuted = !isMuted)
                        }
                    }
                }
            }
        } finally {
            // TODO this finally block should be when calling addCall once it implements
            //   the CoroutineContext
            Log.d("MPB", "Exit scope")
            _currentCall.update {
                TelecomCall.None
            }
        }
    }

    /**
     * Collects changes in the call status to coordinate call related actors like showing the
     * notification for the call, start/stop audio...
     */
    private suspend fun processCallStatus() {
        _currentCall.collect { call ->
            Log.d("MPB", "Call status changed: $call")
            notificationManager.updateCallNotification(call)

            when (call) {
                TelecomCall.None, is TelecomCall.Unregistered -> {
                    audioLoopSource.stopAudioLoop()
                }

                is TelecomCall.Registered -> {
                    if (call.isActive && !call.isOnHold && !call.isMuted) {
                        audioLoopSource.startAudioLoop()
                    } else {
                        audioLoopSource.stopAudioLoop()
                    }
                }
            }
        }
    }

    /**
     * Update the current state of our call applying the transform lambda only if the call is
     * registered. Otherwise keep the current state
     */
    private fun updateCurrentCall(transform: TelecomCall.Registered.() -> TelecomCall) {
        _currentCall.update { call ->
            if (call is TelecomCall.Registered) {
                call.transform()
            } else {
                call
            }
        }
    }

    private suspend fun CallControlScope.doSwitchEndpoint(action: TelecomCallAction.SwitchAudioType) {
        // TODO once availableCallEndpoints is a state flow we can just get the value
        val endpoints = (_currentCall.value as TelecomCall.Registered).availableCallEndpoints

        // Switch to the given endpoint or fallback to the best possible one.
        val newEndpoint = endpoints.firstOrNull { it.type == action.type }
            ?: endpoints.firstOrNull {
                it.type == CallEndpointCompat.TYPE_BLUETOOTH
            } ?: endpoints.firstOrNull {
                it.type == CallEndpointCompat.TYPE_WIRED_HEADSET
            } ?: endpoints.firstOrNull {
                it.type == CallEndpointCompat.TYPE_EARPIECE
            } ?: endpoints.firstOrNull()

        if (newEndpoint != null) {
            requestEndpointChange(newEndpoint).also {
                Log.d("MPB", "Endpoint ${newEndpoint.name} changed: $it")
            }
        }
    }

    private suspend fun CallControlScope.doDisconnect(action: TelecomCallAction.Disconnect) {
        disconnect(action.cause)
        updateCurrentCall {
            TelecomCall.Unregistered(id, callAttributes, action.cause)
        }
    }

    private suspend fun CallControlScope.doAnswer() {
        if (answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)) {
            updateCurrentCall {
                copy(isActive = true, isOnHold = false)
            }
        } else {
            updateCurrentCall {
                TelecomCall.Unregistered(
                    id = id,
                    callAttributes = callAttributes,
                    disconnectCause = DisconnectCause(DisconnectCause.BUSY),
                )
            }
        }
    }
}
