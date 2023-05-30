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

package com.example.platform.ui.insets

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Immersive mode",
    description = "Immersive mode enables your app to display full-screen by hiding system bars.",
    documentation = "https://developer.android.com/develop/ui/views/layout/immersive"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveMode() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { LargeTopAppBar(title = { Text(text = "Immersive mode") }) }
    ) { innerPadding ->
        ImmersiveModeContent(contentPadding = innerPadding)
    }
}

private interface RadioOption {
    val title: String
    val value: Int
}

/** All the available options for the behavior of WindowInsetsController. */
private enum class BehaviorOption(
    override val title: String,
    override val value: Int
) : RadioOption {
    // Swipe from the edge to show a hidden bar. Gesture navigation works regardless of visibility
    // of the navigation bar.
    Default(
        title = "BEHAVIOR_DEFAULT",
        value = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    ),

    // "Sticky immersive mode". Swipe from the edge to temporarily reveal the hidden bar.
    ShowTransientBarsBySwipe(
        title = "BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE",
        value = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    )
}

/** All the available options for the type to hide or show with WindowInsetsController. */
private enum class TypeOption(
    override val title: String,
    override val value: Int
) : RadioOption {
    SystemBars("systemBars()", WindowInsetsCompat.Type.systemBars()),
    StatusBars("statusBars()", WindowInsetsCompat.Type.statusBars()),
    NavigationBars("navigationBars()", WindowInsetsCompat.Type.navigationBars())
}

@Composable
private fun ImmersiveModeContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WindowInsetsController can hide or show specified system bars.
        val window = (LocalContext.current as Activity).window
        val view = LocalView.current
        val controller = remember(key1 = window, key2 = view) {
            WindowInsetsControllerCompat(window, view)
        }

        // The behavior of the immersive mode.
        var behavior by rememberSaveable { mutableStateOf(BehaviorOption.Default) }
        RadioGroup(
            title = "Behavior",
            options = BehaviorOption.values().toList(),
            selected = behavior,
            onSelected = { behavior = it as BehaviorOption }
        )

        // The type of system bars to hide or show.
        var type by rememberSaveable { mutableStateOf(TypeOption.SystemBars) }
        RadioGroup(
            title = "Type",
            options = TypeOption.values().toList(),
            selected = type,
            onSelected = { type = it as TypeOption }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                // Set the behavior.
                controller.systemBarsBehavior = behavior.value
                // Hide the selected type.
                controller.hide(type.value)
            }) {
                Text(text = "HIDE")
            }
            Button(onClick = {
                // Set the behavior.
                controller.systemBarsBehavior = behavior.value
                // Show the selected type.
                controller.show(type.value)
            }) {
                Text(text = "SHOW")
            }
        }

        // Reset
        DisposableEffect(key1 = Unit) {
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
private fun RadioGroup(
    title: String,
    options: List<RadioOption>,
    selected: RadioOption,
    onSelected: (RadioOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.selectableGroup()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        for (option in options) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = option == selected,
                        onClick = { onSelected(option) }
                    )
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == selected,
                    onClick = null
                )
                Text(
                    text = option.title,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
