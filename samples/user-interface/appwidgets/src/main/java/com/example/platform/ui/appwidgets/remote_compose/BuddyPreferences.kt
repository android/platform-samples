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
    val tapsCount: Int = 0,
    val coins: Int = 0,
) {
    val appWidgetId: Int
        get() = widgetId
}

/**
 * Persists per-widget tap counts and collected coins in [SharedPreferences].
 */
object BuddyPreferences {
    const val PREFS_NAME = "buddy_prefs"
    private const val KEY_TAPS_PREFIX = "taps_count_"
    private const val KEY_COINS_PREFIX = "widget_coins_"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTapsCount(context: Context, widgetId: Int): Int {
        val p = prefs(context)
        val tapsKey = "$KEY_TAPS_PREFIX$widgetId"
        return if (p.contains(tapsKey)) {
            p.getInt(tapsKey, 0)
        } else {
            p.getInt("$KEY_COINS_PREFIX$widgetId", 0)
        }
    }

    fun saveTapsCount(context: Context, widgetId: Int, count: Int) {
        prefs(context).edit()
            .putInt("$KEY_TAPS_PREFIX$widgetId", count.coerceAtLeast(0))
            .apply()
    }

    fun addTapsCount(context: Context, widgetId: Int, delta: Int = 1): Int {
        val updated = (getTapsCount(context, widgetId) + delta).coerceAtLeast(0)
        saveTapsCount(context, widgetId, updated)
        return updated
    }

    fun getCoinsForWidget(context: Context, widgetId: Int): Int {
        val p = prefs(context)
        val coinsKey = "$KEY_COINS_PREFIX$widgetId"
        return if (p.contains(coinsKey)) {
            p.getInt(coinsKey, 0)
        } else {
            p.getInt("$KEY_TAPS_PREFIX$widgetId", 0)
        }
    }

    fun saveCoinsForWidget(context: Context, widgetId: Int, coins: Int) {
        prefs(context).edit()
            .putInt("$KEY_COINS_PREFIX$widgetId", coins.coerceAtLeast(0))
            .apply()
    }

    fun addCoinsForWidget(context: Context, widgetId: Int, amount: Int = 1): Int {
        val updated = (getCoinsForWidget(context, widgetId) + amount).coerceAtLeast(0)
        saveCoinsForWidget(context, widgetId, updated)
        return updated
    }

    fun getWidgetBuddyState(context: Context, widgetId: Int): WidgetBuddyState =
        WidgetBuddyState(
            widgetId = widgetId,
            tapsCount = getTapsCount(context, widgetId),
            coins = getCoinsForWidget(context, widgetId),
        )

    fun removeWidgetState(context: Context, widgetId: Int) {
        prefs(context).edit()
            .remove("$KEY_TAPS_PREFIX$widgetId")
            .remove("$KEY_COINS_PREFIX$widgetId")
            .apply()
    }

    /**
     * Returns all active widget IDs from [AppWidgetManager].
     */
    fun getActiveWidgetIds(context: Context): List<Int> {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AndroidPetWidget::class.java)
        return (manager.getAppWidgetIds(component) ?: IntArray(0))
            .filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            .sorted()
    }

    fun getActiveBuddyStates(context: Context): List<WidgetBuddyState> =
        getActiveWidgetIds(context).map { id ->
            getWidgetBuddyState(context, id)
        }

    fun getActiveBuddies(context: Context): List<WidgetBuddyState> =
        getActiveBuddyStates(context)
}

/**
 * Repository wrapper over [BuddyPreferences] and [AppWidgetManager] for querying and updating
 * Android Pet widget state.
 */
class BuddyRepository(context: Context) {
    private val appContext = context.applicationContext ?: context

    fun getActiveWidgetIds(): List<Int> = BuddyPreferences.getActiveWidgetIds(appContext)

    fun getActiveBuddies(): List<Int> = BuddyPreferences.getActiveWidgetIds(appContext)

    fun getActiveBuddyStates(): List<WidgetBuddyState> =
        BuddyPreferences.getActiveBuddyStates(appContext)

    fun getWidgetBuddyState(widgetId: Int): WidgetBuddyState =
        BuddyPreferences.getWidgetBuddyState(appContext, widgetId)

    fun getCoinsForWidget(widgetId: Int): Int =
        BuddyPreferences.getCoinsForWidget(appContext, widgetId)

    fun addCoinsForWidget(widgetId: Int, amount: Int = 1): Int =
        BuddyPreferences.addCoinsForWidget(appContext, widgetId, amount)

    fun playWithBuddy(widgetId: Int): Int {
        val updated = BuddyPreferences.addCoinsForWidget(appContext, widgetId, 1)
        BuddyPreferences.saveTapsCount(appContext, widgetId, updated)
        return updated
    }

    fun getTapsCount(widgetId: Int): Int =
        BuddyPreferences.getTapsCount(appContext, widgetId)

    fun saveTapsCount(widgetId: Int, count: Int) =
        BuddyPreferences.saveTapsCount(appContext, widgetId, count)

    fun removeWidget(widgetId: Int) =
        BuddyPreferences.removeWidgetState(appContext, widgetId)

    fun removeWidgetState(widgetId: Int) =
        BuddyPreferences.removeWidgetState(appContext, widgetId)
}
