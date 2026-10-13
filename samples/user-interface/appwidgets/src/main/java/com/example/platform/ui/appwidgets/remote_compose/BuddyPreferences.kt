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
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences

/**
 * Snapshot of an Android Pet widget instance's state.
 */
data class WidgetBuddyState(
    val widgetId: Int,
    val coins: Int = 0,
)

/**
 * Persists per-widget collected coins in [SharedPreferences].
 */
object BuddyPreferences {
    const val PREFS_NAME = "buddy_prefs"
    private const val KEY_COINS_PREFIX = "widget_coins_"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCoinsForWidget(context: Context, widgetId: Int): Int =
        prefs(context).getInt("$KEY_COINS_PREFIX$widgetId", 0)

    fun addCoinsForWidget(context: Context, widgetId: Int, amount: Int = 1): Int {
        val updated = (getCoinsForWidget(context, widgetId) + amount).coerceAtLeast(0)
        prefs(context).edit()
            .putInt("$KEY_COINS_PREFIX$widgetId", updated)
            .apply()
        return updated
    }

    fun removeWidgetState(context: Context, widgetId: Int) {
        prefs(context).edit()
            .remove("$KEY_COINS_PREFIX$widgetId")
            .apply()
    }

    fun getActiveBuddyStates(context: Context): List<WidgetBuddyState> {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AndroidPetWidget::class.java)
        return (manager.getAppWidgetIds(component) ?: IntArray(0))
            .filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            .sorted()
            .map { id ->
                WidgetBuddyState(
                    widgetId = id,
                    coins = getCoinsForWidget(context, id),
                )
            }
    }
}
