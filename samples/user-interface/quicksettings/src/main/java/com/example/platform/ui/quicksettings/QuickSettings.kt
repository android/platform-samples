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

package com.example.platform.ui.quicksettings

import android.app.StatusBarManager
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.google.android.catalog.framework.annotations.Sample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RequiresApi(24)
@Sample(
    name = "Quick Settings",
    description = "Add your custom tile to the Quick Settings.",
    documentation = "https://developer.android.com/develop/ui/views/quicksettings-tiles",
)
@Composable
fun QuickSettings() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // Whether the tile has been added to the Quick Settings.
        val addedFlow = remember { context.dataStore.data.map { it[TILE_ADDED] ?: false } }
        val added by addedFlow.collectAsState(initial = false)

        if (added) {
            Text(text = "The sample tile has been added to the Quick Settings.")
        } else if (Build.VERSION.SDK_INT >= 33) {
            // On API level 33 and above, we can request to the system that our tile should
            // be added to the Quick Settings.
            val executor = rememberExecutor()
            Button(onClick = { addTile(context, executor, coroutineScope) }) {
                Text(text = "ADD TILE TO QUICK SETTINGS")
            }
        } else {
            Text(text = "Open Quick Settings and add \"Sample Tile\".")
        }

        // The tile is toggleable. This state represents whether it's currently active or not.
        val activeFlow = remember { context.dataStore.data.map { it[TILE_ACTIVE] ?: false } }
        val active by activeFlow.collectAsState(initial = false)

        Text(text = "The state of this switch is synchronized with the tile state.")
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Toggle Active/Inactive")
            Switch(
                checked = active,
                onCheckedChange = { checked ->
                    // Modify the state. The same state is shared between this switch and the tile.
                    coroutineScope.launch {
                        context.dataStore.edit { it[TILE_ACTIVE] = checked }
                    }
                    val componentName = SampleTileService.getComponentName(context)
                    // Request to the system that the tile should catch this state change.
                    TileService.requestListeningState(context, componentName)
                },
            )
        }
    }
}

@RequiresApi(33)
private fun addTile(context: Context, executor: Executor, coroutineScope: CoroutineScope) {
    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    val componentName = SampleTileService.getComponentName(context)
    // Request to the system that our tile should be added to the Quick Settings.
    // This opens up a system dialog, and the tile is added upon the user's approval.
    statusBarManager.requestAddTileService(
        componentName,
        context.getString(R.string.tile_label),
        SampleTileService.getIcon(context),
        executor,
    ) { result ->
        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
            result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
        ) {
            // Record that the tile has been added.
            coroutineScope.launch {
                context.dataStore.edit { it[TILE_ADDED] = true }
            }
            // Request to the system that tile should be updated to the latest state.
            TileService.requestListeningState(context, componentName)
        }
    }
}

@Composable
private fun rememberExecutor(): Executor {
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }
    return executor
}
