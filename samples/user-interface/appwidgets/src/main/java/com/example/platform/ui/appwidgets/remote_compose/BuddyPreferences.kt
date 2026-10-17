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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.core.content.edit
import com.example.platform.ui.appwidgets.R

/**
 * Snapshot of an Android Pet widget instance's configuration and coin state.
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
    val eyeMouthColor: Int = BuddyPreferences.resolveEyeMouthColor(buddyColor),
    val isGrownUp: Boolean = false,
) {
    companion object {
        val Saver: Saver<WidgetBuddyState, Any> = listSaver(
            save = {
                listOf(
                    it.widgetId,
                    it.coins,
                    it.buddyName,
                    it.nickname,
                    it.eyes,
                    it.mouth,
                    it.environment,
                    it.buddyColor,
                    it.eyeMouthColor,
                    it.isGrownUp,
                )
            },
            restore = {
                WidgetBuddyState(
                    widgetId = it[0] as Int,
                    coins = it[1] as Int,
                    buddyName = it[2] as String,
                    nickname = it[3] as String,
                    eyes = it[4] as String,
                    mouth = it[5] as String,
                    environment = it[6] as String,
                    buddyColor = it[7] as Int,
                    eyeMouthColor = it[8] as Int,
                    isGrownUp = it[9] as Boolean,
                )
            },
        )
    }
}

/**
 * Persists per-widget pet configuration and collected coins in [SharedPreferences].
 */
object BuddyPreferences {
    const val PREFS_NAME = "buddy_prefs"
    private const val KEY_COINS_PREFIX = "widget_coins_"
    private const val KEY_BUDDY_PREFIX = "buddy_for_widget_"
    private const val KEY_NICKNAME_PREFIX = "buddy_nickname_for_widget_"
    private const val KEY_EYES_PREFIX = "eyes_for_widget_"
    private const val KEY_MOUTH_PREFIX = "mouth_for_widget_"
    private const val KEY_ENVIRONMENT_PREFIX = "env_for_widget_"
    private const val KEY_BUDDY_COLOR_PREFIX = "buddy_color_for_widget_"
    private const val KEY_EYE_MOUTH_COLOR_PREFIX = "eye_mouth_color_for_widget_"

    const val DEFAULT_BUDDY = "guy_bunny"
    const val DEFAULT_BUDDY_NAME = "Android Buddy"
    const val DEFAULT_EYES = "eyes_1"
    const val DEFAULT_MOUTH = "mouth_1"
    const val DEFAULT_ENVIRONMENT = "env_room"
    const val COLOR_ORIGINAL = 0
    const val COLOR_ANDROID_GREEN = 0xFF34A853.toInt()
    const val DEFAULT_BUDDY_COLOR = COLOR_ORIGINAL
    const val DEFAULT_EYE_MOUTH_COLOR = 0xFF000000.toInt()

    const val EYES_CENTER_Y_FRACTION = 0.50f
    const val MOUTH_OFFSET_Y_FRACTION = 0.08f
    const val MOUTH_CENTER_Y_FRACTION = EYES_CENTER_Y_FRACTION + MOUTH_OFFSET_Y_FRACTION

    val BUDDY_OPTIONS: List<String> = listOf("guy_bunny", "guy_bun", "guy_wiggle")
    val EYE_OPTIONS: List<String> = listOf("eyes_1", "eyes_2", "eyes_4", "eyes_5")
    val MOUTH_OPTIONS: List<String> = listOf("mouth_1", "mouth_2")
    val ENVIRONMENT_OPTIONS: List<String> = listOf("env_room", "env_meadow")

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCoinsForWidget(context: Context, widgetId: Int): Int =
        prefs(context).getInt("$KEY_COINS_PREFIX$widgetId", 0)

    fun addCoinsForWidget(context: Context, widgetId: Int, amount: Int = 1): Int {
        val updated = (getCoinsForWidget(context, widgetId) + amount).coerceAtLeast(0)
        prefs(context).edit {
            putInt("$KEY_COINS_PREFIX$widgetId", updated)
        }
        return updated
    }

    fun saveBuddyConfig(context: Context, state: WidgetBuddyState) {
        val widgetId = state.widgetId
        val resolvedName = state.nickname.trim().ifEmpty { DEFAULT_BUDDY_NAME }
        prefs(context).edit {
            putString("$KEY_BUDDY_PREFIX$widgetId", state.buddyName)
            putString("$KEY_NICKNAME_PREFIX$widgetId", resolvedName)
            putString("$KEY_EYES_PREFIX$widgetId", state.eyes)
            putString("$KEY_MOUTH_PREFIX$widgetId", state.mouth)
            putString("$KEY_ENVIRONMENT_PREFIX$widgetId", state.environment)
            putInt("$KEY_BUDDY_COLOR_PREFIX$widgetId", state.buddyColor)
            putInt("$KEY_EYE_MOUTH_COLOR_PREFIX$widgetId", state.eyeMouthColor)
        }
    }

    fun getWidgetBuddyState(context: Context, widgetId: Int): WidgetBuddyState {
        val p = prefs(context)
        val buddyColor = p.getInt("$KEY_BUDDY_COLOR_PREFIX$widgetId", DEFAULT_BUDDY_COLOR)
        val eyeKey = "$KEY_EYE_MOUTH_COLOR_PREFIX$widgetId"
        val eyeMouthColor = if (p.contains(eyeKey)) {
            val saved = p.getInt(eyeKey, DEFAULT_EYE_MOUTH_COLOR)
            val isNearWhite = ((saved ushr 16) and 0xFF) >= 225 &&
                ((saved ushr 8) and 0xFF) >= 225 &&
                (saved and 0xFF) >= 225
            if (isNearWhite) resolveEyeMouthColor(buddyColor) else saved
        } else {
            resolveEyeMouthColor(buddyColor)
        }
        return WidgetBuddyState(
            widgetId = widgetId,
            coins = p.getInt("$KEY_COINS_PREFIX$widgetId", 0),
            buddyName = p.getString("$KEY_BUDDY_PREFIX$widgetId", DEFAULT_BUDDY) ?: DEFAULT_BUDDY,
            nickname = p.getString("$KEY_NICKNAME_PREFIX$widgetId", null)
                ?.trim()
                ?.ifEmpty { null } ?: DEFAULT_BUDDY_NAME,
            eyes = p.getString("$KEY_EYES_PREFIX$widgetId", DEFAULT_EYES) ?: DEFAULT_EYES,
            mouth = p.getString("$KEY_MOUTH_PREFIX$widgetId", DEFAULT_MOUTH) ?: DEFAULT_MOUTH,
            environment = p.getString("$KEY_ENVIRONMENT_PREFIX$widgetId", DEFAULT_ENVIRONMENT)
                ?: DEFAULT_ENVIRONMENT,
            buddyColor = buddyColor,
            eyeMouthColor = eyeMouthColor,
        )
    }

    fun removeWidgetState(context: Context, widgetId: Int) {
        prefs(context).edit {
            remove("$KEY_COINS_PREFIX$widgetId")
            remove("$KEY_BUDDY_PREFIX$widgetId")
            remove("$KEY_NICKNAME_PREFIX$widgetId")
            remove("$KEY_EYES_PREFIX$widgetId")
            remove("$KEY_MOUTH_PREFIX$widgetId")
            remove("$KEY_ENVIRONMENT_PREFIX$widgetId")
            remove("$KEY_BUDDY_COLOR_PREFIX$widgetId")
            remove("$KEY_EYE_MOUTH_COLOR_PREFIX$widgetId")
        }
    }

    fun getActiveBuddyStates(context: Context): List<WidgetBuddyState> {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, AndroidPetWidget::class.java)
        return (manager.getAppWidgetIds(component) ?: IntArray(0))
            .filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
            .sorted()
            .map { id -> getWidgetBuddyState(context, id) }
    }

    /**
     * Resolves the tint color for eyes and mouth based on the selected buddy vibe color.
     * - Original ([COLOR_ORIGINAL]) and Android Green ([COLOR_ANDROID_GREEN]) use pure black (`0xFF000000`).
     * - System theme colors use [explicitAccentColor] if provided, non-black, and not near-white,
     *   or a contrasting dark shade derived from [buddyColor].
     */
    fun resolveEyeMouthColor(buddyColor: Int, explicitAccentColor: Int = 0): Int {
        if (buddyColor == COLOR_ORIGINAL || buddyColor == COLOR_ANDROID_GREEN) {
            return DEFAULT_EYE_MOUTH_COLOR
        }
        if (explicitAccentColor != 0 && explicitAccentColor != DEFAULT_EYE_MOUTH_COLOR) {
            val isNearWhite = ((explicitAccentColor ushr 16) and 0xFF) >= 225 &&
                ((explicitAccentColor ushr 8) and 0xFF) >= 225 &&
                (explicitAccentColor and 0xFF) >= 225
            if (!isNearWhite) {
                return explicitAccentColor
            }
        }
        val r = ((buddyColor shr 16) and 0xFF) * 35 / 100
        val g = ((buddyColor shr 8) and 0xFF) * 35 / 100
        val b = (buddyColor and 0xFF) * 35 / 100
        val derived = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        return if (derived == DEFAULT_EYE_MOUTH_COLOR) 0xFF1B1B1F.toInt() else derived
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
            "eyes_4" -> R.drawable.eyes_4
            "eyes_5" -> R.drawable.eyes_5
            else -> R.drawable.eyes_1
        }

    fun getMouthDrawableRes(
        mouthStyle: String = DEFAULT_MOUTH,
        isGrownUp: Boolean = false,
    ): Int? {
        if (!isGrownUp) return null
        return if (mouthStyle == "mouth_2") R.drawable.mouth_2 else R.drawable.mouth_1
    }

    fun getDrawableResForEnvironment(envName: String): Int =
        when (envName) {
            "env_meadow" -> R.drawable.env_meadow
            else -> R.drawable.env_room
        }
}
