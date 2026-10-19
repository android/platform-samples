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
import android.graphics.Color
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import com.example.platform.ui.appwidgets.R
import java.io.Serializable

/**
 * Holds the configuration and coin count for one [AndroidPetWidget] instance.
 */
data class WidgetBuddyState(
    val widgetId: Int,
    val coins: Int = 0,
    val buddyName: String = BuddyPreferences.DEFAULT_BUDDY,
    val nickname: String = BuddyPreferences.DEFAULT_BUDDY_NAME,
    val eyes: String = BuddyPreferences.DEFAULT_EYES,
    val mouth: String = BuddyPreferences.DEFAULT_MOUTH,
    val environment: String = BuddyPreferences.DEFAULT_ENVIRONMENT,
    val buddyColor: Int = BuddyPreferences.DEFAULT_BUDDY_COLOR,
    val isGrownUp: Boolean = false,
) : Serializable {
    val eyeMouthColor: Int
        get() = BuddyPreferences.resolveEyeMouthColor(buddyColor)
}

/**
 * Saves and loads per-widget pet configuration and coin counts in [SharedPreferences].
 */
object BuddyPreferences {
    const val PREFS_NAME = "buddy_prefs"
    private val WIDGET_KEYS = listOf("coins", "buddy", "nickname", "eyes", "mouth", "env", "buddy_color")

    const val DEFAULT_BUDDY = "guy_bunny"
    const val DEFAULT_BUDDY_NAME = "Android Buddy"
    const val DEFAULT_EYES = "eyes_1"
    const val DEFAULT_MOUTH = "mouth_1"
    const val DEFAULT_ENVIRONMENT = "env_room"
    const val DEFAULT_BUDDY_COLOR = 0
    const val COLOR_ANDROID_GREEN = 0xFF34A853.toInt()
    const val DEFAULT_EYE_MOUTH_COLOR = Color.BLACK

    const val EYES_CENTER_Y_FRACTION = 0.50f
    const val MOUTH_CENTER_Y_FRACTION = 0.58f

    val BUDDY_OPTIONS: List<String> = listOf("guy_bunny", "guy_bun", "guy_wiggle")
    val EYE_OPTIONS: List<String> = listOf("eyes_1", "eyes_2", "eyes_3", "eyes_4")
    val MOUTH_OPTIONS: List<String> = listOf("mouth_1", "mouth_2")
    val ENVIRONMENT_OPTIONS: List<String> = listOf("env_room", "env_meadow")

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Adds [amount] coins to the balance for [widgetId] and returns the new total.
     */
    fun addCoinsForWidget(context: Context, widgetId: Int, amount: Int = 1): Int {
        val p = prefs(context)
        val key = "coins_$widgetId"
        val updated = (p.getInt(key, 0) + amount).coerceAtLeast(0)
        p.edit { putInt(key, updated) }
        return updated
    }

    /**
     * Saves the pet configuration from [state] for its widget ID.
     */
    fun saveBuddyConfig(context: Context, state: WidgetBuddyState) {
        val id = state.widgetId
        val resolvedName = state.nickname.trim().ifEmpty { DEFAULT_BUDDY_NAME }
        prefs(context).edit {
            putString("buddy_$id", state.buddyName)
            putString("nickname_$id", resolvedName)
            putString("eyes_$id", state.eyes)
            putString("mouth_$id", state.mouth)
            putString("env_$id", state.environment)
            putInt("buddy_color_$id", state.buddyColor)
        }
    }

    /**
     * Loads the saved [WidgetBuddyState] for [widgetId], or returns default values if not set.
     */
    fun getWidgetBuddyState(context: Context, widgetId: Int): WidgetBuddyState {
        val p = prefs(context)
        return WidgetBuddyState(
            widgetId = widgetId,
            coins = p.getInt("coins_$widgetId", 0),
            buddyName = p.getString("buddy_$widgetId", DEFAULT_BUDDY) ?: DEFAULT_BUDDY,
            nickname = p.getString("nickname_$widgetId", DEFAULT_BUDDY_NAME) ?: DEFAULT_BUDDY_NAME,
            eyes = p.getString("eyes_$widgetId", DEFAULT_EYES) ?: DEFAULT_EYES,
            mouth = p.getString("mouth_$widgetId", DEFAULT_MOUTH) ?: DEFAULT_MOUTH,
            environment = p.getString("env_$widgetId", DEFAULT_ENVIRONMENT) ?: DEFAULT_ENVIRONMENT,
            buddyColor = p.getInt("buddy_color_$widgetId", DEFAULT_BUDDY_COLOR),
        )
    }

    /**
     * Deletes all saved preferences for [widgetId] when the user removes the widget.
     */
    fun removeWidgetState(context: Context, widgetId: Int) {
        prefs(context).edit {
            WIDGET_KEYS.forEach { remove("${it}_$widgetId") }
        }
    }

    /**
     * Returns the [WidgetBuddyState] list for all active [AndroidPetWidget] instances on the device.
     */
    fun getActiveBuddyStates(context: Context): List<WidgetBuddyState> {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AndroidPetWidget::class.java)
        return (manager.getAppWidgetIds(component) ?: IntArray(0))
            .filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            .sorted()
            .map { id -> getWidgetBuddyState(context, id) }
    }

    /**
     * Selects a high-contrast color for the eyes and mouth based on [buddyColor].
     * Uses pure black for the default and Android Green themes, and blends [buddyColor]
     * with black for Material theme colors.
     */
    fun resolveEyeMouthColor(buddyColor: Int): Int =
        if (buddyColor == DEFAULT_BUDDY_COLOR || buddyColor == COLOR_ANDROID_GREEN) {
            DEFAULT_EYE_MOUTH_COLOR
        } else {
            ColorUtils.blendARGB(buddyColor, Color.BLACK, 0.65f)
        }

    fun getBodyDrawableResForBuddy(buddyName: String): Int =
        when (buddyName) {
            "guy_bun" -> R.drawable.guy_bun_body
            "guy_wiggle" -> R.drawable.guy_wiggle_body
            else -> R.drawable.guy_bunny_body
        }

    fun getEyesDrawableRes(eyesStyle: String = DEFAULT_EYES): Int =
        when (eyesStyle) {
            "eyes_2" -> R.drawable.eyes_2
            "eyes_3" -> R.drawable.eyes_3
            "eyes_4" -> R.drawable.eyes_4
            else -> R.drawable.eyes_1
        }

    fun getMouthDrawableRes(mouthStyle: String = DEFAULT_MOUTH): Int =
        if (mouthStyle == "mouth_2") R.drawable.mouth_2 else R.drawable.mouth_1

    fun getDrawableResForEnvironment(envName: String): Int =
        if (envName == "env_meadow") R.drawable.env_meadow else R.drawable.env_room
}
