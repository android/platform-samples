package com.example.platform.ui.appwidgets.glance.layout.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit tests for [ImageUtils] (`samples/user-interface/appwidgets/src/test/`).
 *
 * Covers:
 * - Essential 3.1: [ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes] (`WindowMetrics` `6 * width * height`)
 * - Essential 3.2: [ImageUtils.decodeSampledBitmapFromResource] two-stage viewable-quality & byte-cap math
 */
@RunWith(AndroidJUnit4::class)
class ImageUtilsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun getMaxWidgetMemoryAllowedSizeInBytes_usesWindowMetrics() {
    val reportedCap = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val expectedRawBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val wm = context.getSystemService(WindowManager::class.java)
      val bounds = wm.currentWindowMetrics.bounds
      6 * bounds.width() * bounds.height()
    } else {
      val dm = context.resources.displayMetrics
      6 * dm.widthPixels * dm.heightPixels
    }

    assertTrue("Memory cap must be positive", reportedCap > 0)
    assertEquals(expectedRawBytes, reportedCap)
  }

  @Test
  fun decodeSampledBitmapFromResource_enforcesByteCapOnAlphaPngPreviewDrawable() {
    // R.drawable.sample_image_grid_preview is a non-opaque alpha PNG (1680x1205 at 420dpi).
    // Native Skia overrides inPreferredConfig = RGB_565 to ARGB_8888 (4 B/px), so Stage 2 must
    // enforce maxBytesPerImage using 4 B/px and inScaled = false so 9 grid tiles stay within budget.
    val perTileCapBytes = ImageUtils.DEFAULT_MAX_BYTES_PER_IMAGE
    val bitmap = ImageUtils.decodeSampledBitmapFromResource(
      res = context.resources,
      resId = R.drawable.sample_image_grid_preview,
      requestedWidth = 1143,
      requestedHeight = 1143,
      preferredConfig = Bitmap.Config.RGB_565,
      maxBytesPerImage = perTileCapBytes,
    )

    assertTrue(
      "Decoded preview PNG (${bitmap.allocationByteCount} B) must fit within per-tile cap ($perTileCapBytes B)",
      bitmap.allocationByteCount <= perTileCapBytes,
    )
    assertTrue(
      "9 grid tiles (${9L * bitmap.allocationByteCount} B) must stay within 9 * DEFAULT_MAX_BYTES_PER_IMAGE",
      9L * bitmap.allocationByteCount <= 9L * ImageUtils.DEFAULT_MAX_BYTES_PER_IMAGE,
    )
  }
}
