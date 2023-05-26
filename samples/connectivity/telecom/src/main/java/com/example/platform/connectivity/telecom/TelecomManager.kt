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

package com.example.platform.connectivity.telecom

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlCallback
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import com.example.platform.connectivity.audio.datasource.AudioLoopSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TelecomManager(private val context: Context, val viewModel: VoipViewModel) {

    companion object {
        const val APP_SCHEME = "MyCustomScheme"
        const val ALL_CALL_CAPABILITIES = (CallAttributesCompat.SUPPORTS_SET_INACTIVE
                or CallAttributesCompat.SUPPORTS_STREAM or CallAttributesCompat.SUPPORTS_TRANSFER)

        // outgoing attributes constants
        const val OUTGOING_NAME = "Darth Maul"
        val OUTGOING_URI: Uri = Uri.fromParts(APP_SCHEME, "", "")

        // Define the minimal set of properties to start an outgoing call
        var OUTGOING_CALL_ATTRIBUTES = CallAttributesCompat(
            OUTGOING_NAME,
            OUTGOING_URI,
            CallAttributesCompat.DIRECTION_OUTGOING,
            ALL_CALL_CAPABILITIES,
        )

        // incoming attributes constants
        const val INCOMING_NAME = "Sundar Pichai"
        val INCOMING_URI: Uri = Uri.fromParts(APP_SCHEME, "", "")

        // Define all possible properties for CallAttributes
        val INCOMING_CALL_ATTRIBUTES =
            CallAttributesCompat(
                INCOMING_NAME,
                INCOMING_URI,
                CallAttributesCompat.DIRECTION_INCOMING,
                CallAttributesCompat.CALL_TYPE_VIDEO_CALL,
                ALL_CALL_CAPABILITIES,
            )
    }

    var callControlScope: CallControlScope? = null
    var fakeCallSession = AudioLoopSource()

    private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)
    var callsManager = CallsManager(context)

    enum class CallState {
        NOCALL,
        INCOMING,
        OUTGOING,
        INCALL
    }

    init {
        var capabilities: @CallsManager.Companion.Capability Int =
            CallsManager.CAPABILITY_BASELINE or CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING or CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
        callsManager.registerAppWithTelecom(capabilities)
    }

    fun makeOutGoingCall() {
        makeCall(OUTGOING_CALL_ATTRIBUTES)
    }

    fun makeIncomingCall() {
        makeCall(INCOMING_CALL_ATTRIBUTES)
    }

    private fun makeCall(callAttributes: CallAttributesCompat) {

        coroutineScope.launch {
            callsManager.addCall(callAttributes) {
                callControlScope = this

                setCallback(callControlCallback)

                availableEndpoints
                    .onEach { viewModel.availableAudioRoutes.value = it }
                    .launchIn(coroutineScope)

                currentCallEndpoint
                    .onEach { viewModel.activeAudioRoute.value = it }
                    .launchIn(coroutineScope)

                isMuted
                    .onEach { viewModel.isMuted.value = it }
                    .launchIn(coroutineScope)

                onCallReady(callAttributes.direction)
            }


            //this will start foreground service
            //callNotificationSource.postOnGoingCall()
        }
    }

    private fun onCallReady(callDirection: Int) {
        coroutineScope.launch {
            if (callDirection == CallAttributesCompat.DIRECTION_INCOMING) {
                viewModel.onCallStateChanged(CallState.INCOMING)
            } else {
                viewModel.onCallStateChanged(CallState.OUTGOING)
            }
        }
    }

    fun onAnswerCall() {
        coroutineScope.launch {
            callControlScope?.let { callControlScope ->
                if (callControlScope.answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)) {
                    startCall()
                } else {
                    //todo update error state
                    endCall()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun onRejectCall() {
        coroutineScope.launch {
            callControlScope?.let {
                it.disconnect(DisconnectCause(DisconnectCause.REJECTED))
                endCall()
            }
        }
    }

    suspend fun setCallActive() : Boolean {
        callControlScope?.let {
            if (it.setActive()) {
               startCall()
            }
        }
        return false
    }

    suspend fun setCallInActive() : Boolean {
        callControlScope?.let {
            if (it.setInactive()) {
                holdCall()
                return true
            }
        }
        return false
    }

    @SuppressLint("NewApi")
    fun hangUp() {
        coroutineScope.launch {
            callControlScope?.disconnect(DisconnectCause(DisconnectCause.LOCAL))
            endCall()
        }
    }

    fun postIncomingcallNotification() {
        //callNotificationSource.postIncomingCall()
    }

    private fun startCall() {
        fakeCallSession.startAudioLoop()
        viewModel.isActive.update { true }
        viewModel.onCallStateChanged(CallState.INCALL)
    }

    private fun endCall() {
        fakeCallSession.stopAudioLoop()
        viewModel.isActive.update { false }
        viewModel.onCallStateChanged(CallState.NOCALL)
    }

    private fun holdCall() {
        fakeCallSession.stopAudioLoop()
        viewModel.isActive.update { false }
        viewModel.currentCallState.update { CallState.INCALL }
    }

    fun setEndpoint(callEndpoint: CallEndpointCompat) {
        coroutineScope.launch {
            callControlScope?.requestEndpointChange(callEndpoint)
        }
    }

    fun toggleMute(b: Boolean) {
        viewModel.isMuted.update { !viewModel.isMuted.value }
    }


    private val callControlCallback = object : CallControlCallback {
        override suspend fun onSetActive(): Boolean {
            startCall()
            return true
        }

        override suspend fun onSetInactive(): Boolean {
            holdCall()
            return true
        }

        override suspend fun onAnswer(callType: Int): Boolean {
            startCall()
            return true
        }

        override suspend fun onDisconnect(disconnectCause: DisconnectCause): Boolean {
            endCall()
            return true
        }
    }
}