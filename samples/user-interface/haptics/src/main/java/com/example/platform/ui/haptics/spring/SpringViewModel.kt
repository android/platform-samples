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

package com.example.platform.ui.haptics.spring

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Vibrator
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.platform.ui.haptics.R

/**
 * ViewModel that handles state logic for Spring route.
 */
class SpringViewModel(
    val messageToUser: String,
    val vibrator: Vibrator,
) : ViewModel() {

    companion object {
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
        fun isSupportedSDK(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
        }

        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
        fun isSupportedOnDevice(context: Context): Boolean {
            if (!isSupportedSDK()) return false
            val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)!!
            return vibrator.areEnvelopeEffectsSupported() ?: false
        }

        /**
         * Factory for SpringViewModel.
         */
        fun provideFactory(
            application: Application,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                var messageToUser = ""
                if (!isSupportedOnDevice(application)) {
                    messageToUser = application.getString(R.string.message_not_supported)
                }
                val vibrator = ContextCompat.getSystemService(application, Vibrator::class.java)!!
                return SpringViewModel(messageToUser = messageToUser, vibrator = vibrator) as T
            }
        }
    }

}
