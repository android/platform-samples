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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

/**
 * Decodes and composites the configured environment and pet bitmaps (tinted body with mesh noise
 * texture, tinted eyes, and tinted mouth) for [AndroidPetWidget].
 */
object PetBitmapRenderer {

    fun renderEnvironmentBitmap(context: Context, environment: String): Bitmap {
        val envRes = BuddyPreferences.getDrawableResForEnvironment(environment)
        return decodeBitmap(context, envRes)
    }

    fun renderPetBitmap(context: Context, state: WidgetBuddyState): Bitmap {
        val bodyRes = BuddyPreferences.getBodyDrawableResForBuddy(state.buddyName)
        val rawBody = decodeBitmap(context, bodyRes)
        val tintedBody = tintBitmap(rawBody, state.buddyColor)
        val texturedBody = applyMeshNoiseTexture(tintedBody, state.buddyName, state.buddyColor)

        val bodyWidth = texturedBody.width.coerceAtLeast(1)
        val bodyHeight = texturedBody.height.coerceAtLeast(1)
        val composite = createBitmap(bodyWidth, bodyHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.drawBitmap(texturedBody, 0f, 0f, paint)

        val rawEyes = decodeBitmap(context, BuddyPreferences.getEyesDrawableRes(state.eyes))
        val tintedEyes = tintFeatureBitmap(rawEyes, state.eyeMouthColor)
        drawFeatureOnBodyCanvas(
            canvas = canvas,
            paint = paint,
            feature = tintedEyes,
            bodyWidth = bodyWidth,
            bodyHeight = bodyHeight,
            widthFraction = 0.65f,
            centerYFraction = BuddyPreferences.EYES_CENTER_Y_FRACTION,
        )

        val rawMouth = decodeBitmap(context, BuddyPreferences.getMouthDrawableRes(state.mouth))
        val tintedMouth = tintFeatureBitmap(rawMouth, state.eyeMouthColor)
        drawFeatureOnBodyCanvas(
            canvas = canvas,
            paint = paint,
            feature = tintedMouth,
            bodyWidth = bodyWidth,
            bodyHeight = bodyHeight,
            widthFraction = 1.0f,
            centerYFraction = BuddyPreferences.MOUTH_CENTER_Y_FRACTION,
        )

        return composite
    }

    fun decodeBitmap(context: Context, resId: Int): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        if (bitmap != null) {
            return bitmap
        }
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: throw IllegalArgumentException("Resource not found: $resId")
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val vectorBitmap = createBitmap(width, height)
        val canvas = Canvas(vectorBitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return vectorBitmap
    }

    fun tintBitmap(source: Bitmap, colorArgb: Int): Bitmap {
        if (colorArgb == 0 || colorArgb == BuddyPreferences.DEFAULT_BUDDY_COLOR) {
            return source
        }
        val width = source.width
        val height = source.height
        val output = createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(colorArgb, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val targetR = (colorArgb ushr 16) and 0xFF
        val targetG = (colorArgb ushr 8) and 0xFF
        val targetB = colorArgb and 0xFF
        var hasNonTransparent = false
        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            if (a != 0) {
                hasNonTransparent = true
                pixels[i] = (a shl 24) or (targetR shl 16) or (targetG shl 8) or targetB
            }
        }
        if (hasNonTransparent) {
            output.setPixels(pixels, 0, width, 0, 0, width, height)
        }
        return output
    }

    /**
     * Tints dark/black pixels of an eye or mouth bitmap to [colorArgb] while preserving white
     * sclera/cutout pixels as pure white (`#FFFFFF`).
     */
    fun tintFeatureBitmap(source: Bitmap, colorArgb: Int): Bitmap {
        val targetColor = if (colorArgb == 0) BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR else colorArgb
        val width = source.width
        val height = source.height
        val output = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetR = (targetColor ushr 16) and 0xFF
        val targetG = (targetColor ushr 8) and 0xFF
        val targetB = targetColor and 0xFF

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel ushr 24) and 0xFF
            if (a == 0) continue

            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF

            val whiteness = minOf(r, g, b)
            val outR = (targetR + (whiteness * (255 - targetR) + 127) / 255).coerceIn(0, 255)
            val outG = (targetG + (whiteness * (255 - targetG) + 127) / 255).coerceIn(0, 255)
            val outB = (targetB + (whiteness * (255 - targetB) + 127) / 255).coerceIn(0, 255)

            pixels[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun drawFeatureOnBodyCanvas(
        canvas: Canvas,
        paint: Paint,
        feature: Bitmap,
        bodyWidth: Int,
        bodyHeight: Int,
        widthFraction: Float,
        centerYFraction: Float,
    ) {
        val targetW = (bodyWidth * widthFraction).coerceAtLeast(1f)
        val aspect = if (feature.width > 0) feature.height.toFloat() / feature.width.toFloat() else 1f
        val targetH = (targetW * aspect).coerceAtLeast(1f)
        val left = (bodyWidth - targetW) / 2f
        val top = (bodyHeight * centerYFraction) - (targetH / 2f)
        val dstRect = RectF(left, top, left + targetW, top + targetH)
        canvas.drawBitmap(feature, null, dstRect, paint)
    }

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

        val (stippleR, stippleG, stippleB) = when {
            buddyColor != 0 && buddyColor != BuddyPreferences.DEFAULT_BUDDY_COLOR -> {
                val r = ((buddyColor ushr 16) and 0xFF) * 0.6f
                val g = ((buddyColor ushr 8) and 0xFF) * 0.6f
                val b = (buddyColor and 0xFF) * 0.6f
                Triple(r.toInt(), g.toInt(), b.toInt())
            }
            buddyName == "guy_bun" -> Triple(0x1F, 0x78, 0x88)
            buddyName == "guy_wiggle" -> Triple(0x5A, 0x82, 0x14)
            else -> Triple(146, 92, 183)
        }

        val seed = 4194
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val pixel = pixels[rowOffset + x]
                val srcA = (pixel ushr 24) and 0xFF
                if (srcA < 10) continue

                var h0 = seed + x * 374761393 + y * 668265263
                h0 = (h0 xor (h0 ushr 13)) * 1274126177
                val n0 = ((h0 xor (h0 ushr 16)) and 0x7FFFFFFF) / 2147483647f

                var h1 = seed + (x shr 1) * 374761393 + (y shr 1) * 668265263 + 101
                h1 = (h1 xor (h1 ushr 13)) * 1274126177
                val n1 = ((h1 xor (h1 ushr 16)) and 0x7FFFFFFF) / 2147483647f

                val noiseVal = n0 * 0.7f + n1 * 0.3f

                if (noiseVal in 0.12f..0.42f) {
                    val grainAlpha = (0.28f + (noiseVal - 0.12f) * 0.35f) * (srcA / 255f)
                    val invAlpha = 1f - grainAlpha

                    val srcR = (pixel ushr 16) and 0xFF
                    val srcG = (pixel ushr 8) and 0xFF
                    val srcB = pixel and 0xFF

                    val outR = (srcR * invAlpha + stippleR * grainAlpha).toInt().coerceIn(0, 255)
                    val outG = (srcG * invAlpha + stippleG * grainAlpha).toInt().coerceIn(0, 255)
                    val outB = (srcB * invAlpha + stippleB * grainAlpha).toInt().coerceIn(0, 255)

                    pixels[rowOffset + x] = (srcA shl 24) or (outR shl 16) or (outG shl 8) or outB
                }
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
