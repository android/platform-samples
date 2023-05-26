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
import android.os.Build
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoipViewModel(context: Context): ViewModel(){
    val isMuted = MutableStateFlow(false)
    val isActive = MutableStateFlow(false)
    val activeAudioRoute : MutableStateFlow<CallEndpointCompat?> = MutableStateFlow(null)
    val availableAudioRoutes : MutableStateFlow<List<CallEndpointCompat>> = MutableStateFlow(emptyList())
    val currentCallState = MutableStateFlow(TelecomManager.CallState.NOCALL)

    var CallerName = "Jane Doe"

    private val telecomManager = TelecomManager(context, this)

    fun onMakeCall(callDirection: Int){
        when(callDirection){
            CallAttributesCompat.DIRECTION_INCOMING -> telecomManager.makeIncomingCall()
            else -> telecomManager.makeOutGoingCall()
        }
    }

    fun toggleHoldCall(toggle: Boolean){
        viewModelScope.launch {
            val hasError = when(toggle) {

                true -> telecomManager.setCallActive()

                false -> telecomManager.setCallInActive()
            }

        }
    }

    fun toggleMute(toggle: Boolean){
        telecomManager.toggleMute(toggle)
    }

    fun answerCall(){
        telecomManager.onAnswerCall()
    }


    fun rejectCall(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            telecomManager.onRejectCall()
        }
    }

    fun disconnectCall(){
        telecomManager.hangUp()
    }

    fun setEndpoint(callEndpointCompat: CallEndpointCompat) {
        telecomManager.setEndpoint(callEndpointCompat)
    }
    fun onCallStateChanged(callState : TelecomManager.CallState){
        currentCallState.update { callState }

        when(callState){
            TelecomManager.CallState.OUTGOING -> {fakeDialingCall()}
            TelecomManager.CallState.INCOMING -> {}
            TelecomManager.CallState.INCALL -> {}
            else -> { onDialerScreen() }
        }
    }

    /**
     * Fake a dialing out Call
     * Waits for 5 Seconds before setting the call to active
     */
    private fun fakeDialingCall(){
        viewModelScope.launch {
            delay(5000)
            telecomManager.setCallActive()
        }
    }

    private fun onDialerScreen() {
        isActive.update { false }
        currentCallState.update { TelecomManager.CallState.NOCALL }
        activeAudioRoute.update { null }
        availableAudioRoutes.update { emptyList() }
    }

    fun onErrorMessage(){

    }
}