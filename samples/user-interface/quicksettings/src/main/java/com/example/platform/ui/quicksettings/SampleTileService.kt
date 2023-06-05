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

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "SampleTileService"

@RequiresApi(24)
class SampleTileService : TileService() {

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, SampleTileService::class.java)
        }

        fun getIcon(context: Context): Icon {
            return Icon.createWithResource(context, R.drawable.ic_android)
        }
    }

    // The coroutine scope that's available from onCreate to onDestroy.
    private var coroutineScope: CoroutineScope? = null

    // The job for observing the state change. Available from onStartListening to onStopListening.
    private var listeningJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        coroutineScope = CoroutineScope(Job() + Dispatchers.Main)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        coroutineScope?.cancel()
    }

    // Called when the tile is added to the Quick Settings by the user.
    // Note that this won't be called when the tile was added by
    // [StatusBarManager.requestAddTileService()].
    override fun onTileAdded() {
        super.onTileAdded()
        Log.d(TAG, "onTileAdded + ${Thread.currentThread().name}")
        coroutineScope?.launch {
            dataStore.edit { it[TILE_ADDED] = true }
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.d(TAG, "onTileRemoved")
        coroutineScope?.launch {
            dataStore.edit { it[TILE_ADDED] = false }
        }
    }

    // Called when the tile should start listening to some state change that it needs to react to.
    // Typically, this is invoked when the app calls [TileService.requestListeningState].
    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening")
        listeningJob = coroutineScope?.launch {
            dataStore.data
                .map { prefs -> prefs[TILE_ACTIVE] ?: false }
                .collect { active -> updateTile(active) }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening")
        listeningJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick")
        coroutineScope?.launch {
            dataStore.edit { prefs ->
                val newState = !(prefs[TILE_ACTIVE] ?: true)
                Log.d(TAG, "New state: $newState")
                prefs[TILE_ACTIVE] = newState
                updateTile(newState)
            }
        }
    }

    private fun updateTile(active: Boolean) {
        val tile = qsTile
        // Update the tile states.
        tile.label = getString(R.string.tile_label)
        tile.icon = getIcon(this)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 30) {
            tile.stateDescription = if (active) "Active" else "Inactive"
        }
        // The state updates won't be reflected until we call updateTile.
        tile.updateTile()
    }
}
