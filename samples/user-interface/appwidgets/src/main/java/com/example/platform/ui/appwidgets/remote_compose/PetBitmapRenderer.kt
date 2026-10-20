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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Decodes and composites the room background and pet bitmaps (body with mesh noise
 * texture, eyes, and evolved mouth) for [AndroidPetWidget].
 */
object PetBitmapRenderer {

    /**
     * Decodes the room background bitmap for [environment].
     */
    fun renderEnvironmentBitmap(context: Context, environment: String): Bitmap {
        val envRes = BuddyPreferences.getDrawableResForEnvironment(environment)
        return decodeBitmap(context, envRes)
    }

    /**
     * Composites the pet body, eyes, and optional evolved mouth into one bitmap.
     */
    fun renderPetBitmap(context: Context, state: WidgetBuddyState): Bitmap {
        val rawBody = decodeBitmap(context, BuddyPreferences.getBodyDrawableResForBuddy(state.buddyName))
        val tintedBody = tintBitmap(rawBody, state.buddyColor)
        val body = applyMeshNoiseTexture(tintedBody, state.buddyName, state.buddyColor)

        val canvas = Canvas(body)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val eyes = decodeBitmap(context, BuddyPreferences.getEyesDrawableRes(state.eyes))
        drawFeature(canvas, paint, eyes, 0.65f, BuddyPreferences.EYES_CENTER_Y_FRACTION)

        if (state.isGrownUp) {
            val mouth = decodeBitmap(context, BuddyPreferences.getMouthDrawableRes(state.mouth))
            drawFeature(canvas, paint, mouth, 1.0f, BuddyPreferences.MOUTH_CENTER_Y_FRACTION)
        }

        return body
    }

    /**
     * Decodes a vector or raster drawable resource into a [Bitmap].
     */
    fun decodeBitmap(context: Context, resId: Int): Bitmap =
        requireNotNull(ContextCompat.getDrawable(context, resId)).toBitmap()

    /**
     * Tints all non-transparent pixels of [source] to [colorArgb].
     */
    fun tintBitmap(source: Bitmap, colorArgb: Int): Bitmap {
        if (colorArgb == BuddyPreferences.DEFAULT_BUDDY_COLOR) {
            return source
        }
        val output = createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(colorArgb, PorterDuff.Mode.SRC_IN)
        }
        Canvas(output).drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Draws [feature] horizontally centered on [canvas] at [centerYFraction] of the canvas height.
     */
    private fun drawFeature(
        canvas: Canvas,
        paint: Paint,
        feature: Bitmap,
        widthFraction: Float,
        centerYFraction: Float,
    ) {
        val targetW = (canvas.width * widthFraction).coerceAtLeast(1f)
        val targetH = (targetW * feature.height / feature.width.coerceAtLeast(1)).coerceAtLeast(1f)
        val left = (canvas.width - targetW) / 2f
        val top = (canvas.height * centerYFraction) - (targetH / 2f)
        canvas.drawBitmap(feature, null, RectF(left, top, left + targetW, top + targetH), paint)
    }

    /**
     * Applies a deterministic stipple grain texture over the pet body bitmap.
     */
    fun applyMeshNoiseTexture(
        source: Bitmap,
        buddyName: String = BuddyPreferences.DEFAULT_BUDDY,
        buddyColor: Int = BuddyPreferences.DEFAULT_BUDDY_COLOR,
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val stippleColor = when {
            buddyColor != BuddyPreferences.DEFAULT_BUDDY_COLOR ->
                ColorUtils.blendARGB(buddyColor, Color.BLACK, 0.4f)
            buddyName == "guy_bun" -> 0xFF1F7888.toInt()
            buddyName == "guy_wiggle" -> 0xFF5A8214.toInt()
            else -> 0xFF925CB7.toInt()
        }

        fun hashNoise(x: Int, y: Int, offset: Int = 0): Float {
            var h = 4194 + x * 374761393 + y * 668265263 + offset
            h = (h xor (h ushr 13)) * 1274126177
            return ((h xor (h ushr 16)) and 0x7FFFFFFF) / 2147483647f
        }

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val srcA = pixel ushr 24
            if (srcA < 10) continue

            val x = i % width
            val y = i / width
            val noiseVal = hashNoise(x, y) * 0.7f + hashNoise(x shr 1, y shr 1, 101) * 0.3f
            if (noiseVal in 0.12f..0.42f) {
                val grainAlpha = (0.28f + (noiseVal - 0.12f) * 0.35f) * (srcA / 255f)
                val blended = ColorUtils.blendARGB(pixel, stippleColor, grainAlpha)
                pixels[i] = (srcA shl 24) or (blended and 0x00FFFFFF)
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
