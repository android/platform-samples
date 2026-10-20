package com.example.platform.ui.appwidgets.glance.layout.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit tests for [ImageUtils] (`samples/user-interface/appwidgets/src/androidTest/`),
 * following the `platform-samples` `connectedDebugAndroidTest` CI convention.
 *
 * Covers:
 * - Essential 3.2: [ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes] (`WindowMetrics` + `15 MiB` ceiling)
 * - Essential 3.3: [ImageUtils.decodeSampledBitmapFromResource] two-stage viewable-quality & byte-cap math
 */
@RunWith(AndroidJUnit4::class)
class ImageUtilsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun getMaxWidgetMemoryAllowedSizeInBytes_usesWindowMetricsAndCapsAt15MiB() {
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
    assertTrue(
      "Memory cap ($reportedCap B) must never exceed 15 MB (15,000,000 B) for foldable cover-screen safety",
      reportedCap <= 15_000_000,
    )
    assertEquals(min(15_000_000, expectedRawBytes), reportedCap)
  }

  @Test
  fun decodeSampledBitmapFromResource_enforcesByteCapOnAlphaPngPreviewDrawable() {
    // Row 8: R.drawable.sample_image_grid_preview is a non-opaque alpha PNG (1680x1205 at 420dpi).
    // Native Skia overrides inPreferredConfig = RGB_565 to ARGB_8888 (4 B/px), so Stage 2 must
    // enforce maxBytesPerImage using 4 B/px so 9 grid tiles never exceed the widget cap.
    val perTileCapBytes = context.getMaxWidgetMemoryAllowedSizeInBytes() / 9
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
      "9 grid tiles (${9L * bitmap.allocationByteCount} B) must stay within 15.55 MB boot cap (15,552,000 B)",
      9L * bitmap.allocationByteCount <= 15_552_000L,
    )
  }
}
