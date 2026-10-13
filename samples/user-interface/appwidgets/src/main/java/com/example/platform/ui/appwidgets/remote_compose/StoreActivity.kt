/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.platform.ui.appwidgets.remote_compose

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.platform.ui.appwidgets.R

/**
 * Companion Store activity for the Remote Compose [AndroidPetWidget].
 * Displays each active widget's ID and collected coin balance.
 */
class StoreActivity : ComponentActivity() {

    private var selectedWidgetId by mutableIntStateOf(AppWidgetManager.INVALID_APPWIDGET_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        selectedWidgetId = extractWidgetId(intent)

        setContent {
            MaterialTheme {
                StoreScreen(
                    selectedWidgetId = selectedWidgetId,
                    onBack = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedWidgetId = extractWidgetId(intent)
    }

    private fun extractWidgetId(intent: Intent?): Int {
        if (intent == null) return AppWidgetManager.INVALID_APPWIDGET_ID
        val fromStandardExtra = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (fromStandardExtra != AppWidgetManager.INVALID_APPWIDGET_ID) {
            return fromStandardExtra
        }
        return intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    companion object {
        const val EXTRA_WIDGET_ID = "appWidgetId"

        fun start(
            context: Context,
            widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
        ) {
            val intent = Intent(context, StoreActivity::class.java).apply {
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra(EXTRA_WIDGET_ID, widgetId)
                }
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    selectedWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    buddyStates: List<WidgetBuddyState>? = null,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var loadedStates by remember(context, selectedWidgetId) {
        mutableStateOf(loadWidgetStates(context, selectedWidgetId))
    }

    DisposableEffect(lifecycleOwner, context, selectedWidgetId, buddyStates) {
        val prefs = BuddyPreferences.prefs(context)
        val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            if (buddyStates == null) {
                loadedStates = loadWidgetStates(context, selectedWidgetId)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && buddyStates == null) {
                loadedStates = loadWidgetStates(context, selectedWidgetId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val activeStates = buddyStates ?: loadedStates

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shop_button),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(text = "Android Pet Store")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (activeStates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "No active Android Pet widgets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Pin an Android Pet widget to your home screen and tap the pet to collect coins.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else {
                items(activeStates, key = { it.widgetId }) { state ->
                    WidgetCoinCard(
                        state = state,
                        isSelected = state.widgetId == selectedWidgetId,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCoinCard(
    state: WidgetBuddyState,
    isSelected: Boolean,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Widget ID: ${state.widgetId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (isSelected) {
                    Text(
                        text = "Active Widget",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.claim_credit_button),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = "Coins: ${state.coins}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun loadWidgetStates(
    context: Context,
    selectedWidgetId: Int,
): List<WidgetBuddyState> {
    val states = BuddyPreferences.getActiveBuddyStates(context).toMutableList()
    if (
        selectedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
        states.none { it.widgetId == selectedWidgetId }
    ) {
        states.add(
            WidgetBuddyState(
                widgetId = selectedWidgetId,
                coins = BuddyPreferences.getCoinsForWidget(context, selectedWidgetId),
            )
        )
    }
    return states.sortedBy { it.widgetId }
}
