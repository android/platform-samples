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

package com.example.platform.privacy.transparency

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.android.catalog.framework.annotations.Sample
import java.text.DateFormat
import java.util.Date

@Sample(
    name = "Screenshot Detection",
    description = "This sample shows how to detect that the user capture the screen in Android 14 onwards",
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ScreenshotDetectionSample : Fragment() {

    // When modified the UI will recompose
    private val stateList = mutableStateListOf<Long>()

    // When registered this callback will be called by the system when it detects a screenshot
    // It does not provide the bitmap or any further information. Only the signal that a screenshot
    // was taken by the user.
    private val screenCaptureCallback = Activity.ScreenCaptureCallback {
        // Add the timestamp to the list
        stateList.add(System.currentTimeMillis())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        // Simple UI that displays the timestamp when a screenshot was detect while the app is
        // in the foreground
        return ComposeView(requireContext()).apply {
            setContent {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            text = "I can detect screenshots. Try it!",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }

                    items(stateList) {
                        val date = DateFormat.getTimeInstance().format(Date(it))
                        Text(
                            text = "$date - Screenshot detected",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The system will only notify the app while in the foreground. Thus we can just register
        // onStart and unregister onStop
        requireActivity().registerScreenCaptureCallback(
            requireActivity().mainExecutor,
            screenCaptureCallback,
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterScreenCaptureCallback(screenCaptureCallback)
    }
}
