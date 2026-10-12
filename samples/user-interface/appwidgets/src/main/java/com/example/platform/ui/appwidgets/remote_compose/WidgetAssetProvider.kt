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
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.platform.ui.appwidgets.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Decodes and caches drawable resources into [Bitmap] instances for use in Remote Compose widgets.
 */
object WidgetAssetProvider {
    private val bitmapCache = ConcurrentHashMap<Int, Bitmap>()

    fun claimCreditBitmap(context: Context): Bitmap =
        decodeBitmap(context, R.drawable.claim_credit_button)

    fun storeBitmap(context: Context): Bitmap =
        decodeBitmap(context, R.drawable.shop_button)

    fun decodeBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
        val appContext = context.applicationContext ?: context
        return bitmapCache.getOrPut(resId) {
            decodeDrawableToBitmap(appContext, resId)
        }
    }

    fun clearCache() {
        bitmapCache.clear()
    }

    private fun decodeDrawableToBitmap(context: Context, @DrawableRes resId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
