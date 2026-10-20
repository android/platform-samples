package com.example.platform.ui.appwidgets.glance.layout.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ImageUtils {

  // Conservative 15 MiB ceiling so bitmaps decoded while a foldable is unfolded (26.81 MB cap)
  // still fit within the cover screen's ~15.71 MB cap when the device is folded closed.
  private const val CONSERVATIVE_MAX_BYTES = 15_728_640

  /**
   * Loads the image resource as bitmap by scaling for the requested size and enforcing
   * the per-image AppWidget memory ceiling.
   * https://developer.android.com/topic/performance/graphics/load-bitmap#load-bitmap
   */
  fun decodeSampledBitmapFromResource(
    res: Resources,
    resId: Int,
    requestedWidth: Int,
    requestedHeight: Int,
    preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565,
    maxBytesPerImage: Int = CONSERVATIVE_MAX_BYTES / 9,
  ): Bitmap {
    val bytesPerPixel = when (preferredConfig) {
      Bitmap.Config.ALPHA_8 -> 1
      Bitmap.Config.RGB_565 -> 2
      else -> 4
    }

    fun calculateInSampleSize(
      originalSize: Size,
      requestedSize: Size,
      effectiveBytesPerPixel: Int = bytesPerPixel,
    ): Int {
      val (height: Int, width: Int) = originalSize.height to originalSize.width
      var inSampleSize = 1
      // Stage 1: Downsample to the highest quality the display can render for the viewable size,
      // and also halve whenever either dimension exceeds 2x the requested dimension (so wide
      // banners or tall portraits do not bypass downsampling when one axis already fits).
      while (
        (height / (inSampleSize * 2) >= requestedSize.height &&
          width / (inSampleSize * 2) >= requestedSize.width) ||
          (height / inSampleSize > requestedSize.height * 2) ||
          (width / inSampleSize > requestedSize.width * 2)
      ) {
        inSampleSize *= 2
      }
      // Stage 2: Enforce the exact per-image OS widget memory ceiling.
      while (
        (max(1, width / inSampleSize).toLong() *
          max(1, height / inSampleSize).toLong() *
          effectiveBytesPerPixel) > maxBytesPerImage
      ) {
        inSampleSize *= 2
      }
      return inSampleSize
    }

    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
      inJustDecodeBounds = true
      BitmapFactory.decodeResource(res, resId, this)
      val originalWidth: Int = outWidth
      val originalHeight: Int = outHeight
      // If the resource has an alpha channel (e.g. non-opaque PNG/WebP), Android's built-in
      // BitmapFactory ignores inPreferredConfig = RGB_565 and sets outConfig = ARGB_8888 (4 B/px).
      val effectiveBytesPerPixel = if (outConfig == Bitmap.Config.ARGB_8888) {
        max(bytesPerPixel, 4)
      } else {
        bytesPerPixel
      }

      inSampleSize =
        calculateInSampleSize(
          originalSize = Size(originalWidth, originalHeight),
          requestedSize = Size(requestedWidth, requestedHeight),
          effectiveBytesPerPixel = effectiveBytesPerPixel,
        )

      // Decode bitmap with inSampleSize and preferredConfig set
      inJustDecodeBounds = false
      inPreferredConfig = preferredConfig

      BitmapFactory.decodeResource(res, resId, this)
    }
  }

  /**
   * Returns maximum bytes allowed per the appwidget limits for bitmaps in the remote views.
   * Uses [WindowManager.getCurrentWindowMetrics] on API 30+ (matching AppWidgetServiceImpl)
   * and caps at [CONSERVATIVE_MAX_BYTES] (15 MiB) for foldable cover-screen safety.
   */
  fun Context.getMaxWidgetMemoryAllowedSizeInBytes(): Int {
    val windowManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      getSystemService(WindowManager::class.java)
    } else {
      null
    }
    val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
      val bounds = windowManager.currentWindowMetrics.bounds
      Size(bounds.width(), bounds.height())
    } else {
      resources.displayMetrics.run { Size(widthPixels, heightPixels) }
    }
    // Cap memory usage at 1.5 times the size of the display
    // 1.5 * 4 bytes/pixel * w * h ==> 6 * w * h
    // See https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/appwidget/java/com/android/server/appwidget/AppWidgetServiceImpl.java;l=274-281;drc=389cb6f54a5a5bb8dea540f57a3a8ac3c3c1c758
    return min(CONSERVATIVE_MAX_BYTES, 6 * size.width * size.height)
  }

  /**
   * Returns maximum possible size for each image in the widget when provided as bitmap.
   */
  fun getMaxPossibleImageSize(aspectRatio: Double, memoryLimitBytes: Int, maxImages: Int): Size {
    // for each orientation (landscape, portrait, +2 for fold).
    val limit = (memoryLimitBytes / 4) / max(1, maxImages)
    val maxSizeAllowedPerPixel: Int = limit / BYTES_PER_PIXEL

    val side = sqrt(maxSizeAllowedPerPixel.toDouble()).toInt()
    val width = if (aspectRatio > 1) side else max(1, (side * aspectRatio).roundToInt())
    val height = if (aspectRatio > 1) max(1, (side / aspectRatio).roundToInt()) else side
    return Size(width, height)
  }

  private const val BYTES_PER_PIXEL = 4
}
