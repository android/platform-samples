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
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.catalog.framework.annotations.Sample
import java.text.DateFormat
import java.util.Date

@Sample(
    name = "Screenshot Detection",
    description = "This sample shows how to detect that the user capture the screen in Android 14 onwards"
)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun ScreenshotDetection() {
    val stateList = remember {
        mutableStateListOf<Long>()
    }
    ScreenCaptureEffect {
        // Store the timestamps when a screenshot is detected.
        stateList.add(System.currentTimeMillis())
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "I can detect screenshots. Try it!",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(stateList) {
            val date = DateFormat.getTimeInstance().format(Date(it))
            Text(
                text = "$date - Screenshot detected",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * An effect for handling screen capture callbacks
 *
 * Calling this in your composable adds the given lambda to the [Activity.ScreenCaptureCallback]
 * from the current activity based on the [LocalLifecycleOwner] and the composition state.
 */
@RequiresApi(34)
@Composable
fun ScreenCaptureEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onScreenCapture: () -> Unit,
) {
    val activity = findActivity()
    val currentOnScreenCapture by rememberUpdatedState(newValue = onScreenCapture)

    DisposableEffect(lifecycleOwner) {
        // Notify the current function when a screenshot is detected
        val screenCaptureCallback = Activity.ScreenCaptureCallback {
            currentOnScreenCapture()
        }

        // Register the screen capture callback onStart and unregister it onStop
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                activity.registerScreenCaptureCallback(activity.mainExecutor, screenCaptureCallback)
            } else if (event == Lifecycle.Event.ON_STOP) {
                activity.unregisterScreenCaptureCallback(screenCaptureCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and the callback
        onDispose {
            activity.unregisterScreenCaptureCallback(screenCaptureCallback)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Utility method to find the current activity inside a composable function
 */
@Composable
private fun findActivity(): Activity {
    var context = LocalContext.current
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("This composable is not part of any activity.")
}
