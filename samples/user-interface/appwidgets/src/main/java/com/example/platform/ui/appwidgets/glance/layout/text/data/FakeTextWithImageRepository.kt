package com.example.layoutsamples.text.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.glance.GlanceId
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.layoutsamples.utils.AspectRatio
import com.example.layoutsamples.utils.AspectRatio.Companion.asDouble
import com.example.layoutsamples.text.layout.ImageData
import com.example.layoutsamples.text.layout.TextData
import com.example.layoutsamples.text.layout.TextWithImageData
import com.example.layoutsamples.utils.ImageUtils
import com.example.layoutsamples.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.layoutsamples.computeIfAbsent as computeIfAbsentExt
/**
 * An fake in-memory repository to provide data for displaying different demo samples in
 * [com.example.layoutsamples.text.layout.TextWithImageLayout]
 */
class FakeTextWithImageRepository {
  private val data = MutableStateFlow<TextWithImageData?>(null)
  private var itemIndex = 0
  private var items = demoItems

  fun data(): Flow<TextWithImageData?> = data

  suspend fun refresh(context: Context) {
    val itemCount = items.size
    itemIndex = (itemIndex + 1) % itemCount

    this.load(context)
  }

  suspend fun load(context: Context): TextWithImageData? {
    val item = items[itemIndex]
    val bitmap = fetchImage(context, item.url)
    val mappedImageData = ImageData(
      bitmap = bitmap,
      contentDescription = item.imageContentDescription
    )

    data.value = TextWithImageData(
      textData = TextData(
        key = "$itemIndex",
        primary = item.primary,
        secondary = item.secondary,
        caption = item.caption
      ),
      imageData = mappedImageData
    )
    return data.value
  }

  private suspend fun fetchImage(context: Context, url: String): Bitmap? {
    val maxAllowedBytes = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val imageSizeLimit = ImageUtils.getMaxPossibleImageSize(
      aspectRatio = AspectRatio.Ratio16x9.asDouble(),
      memoryLimitBytes = maxAllowedBytes,
      maxImages = 1
    )
    val maxWidth = imageSizeLimit.width
    val maxHeight = imageSizeLimit.height

    var bitmap: Bitmap? = null

    val result = ImageLoader(context).execute(
      ImageRequest.Builder(context)
        .data(url)
        .size(maxWidth, maxHeight)
        .target { res: Drawable ->
          bitmap = (res as BitmapDrawable).bitmap
        }.build()
    )

    if (result is ErrorResult) {
      Log.e(TAG, "Failed to load the image:", result.throwable)
    }

    return bitmap
  }

  companion object {
    private const val TAG = "FTWIR"

    private val repositories = mutableMapOf<GlanceId, FakeTextWithImageRepository>()

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getRepo(glanceId: GlanceId): FakeTextWithImageRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsentExt(glanceId) {
          FakeTextWithImageRepository()
        }!!
      }
    }

    /**
     * Cleans up local data associated with the provided [glanceId].
     */
    fun cleanUp(glanceId: GlanceId) {
      synchronized(repositories) {
        repositories.remove(glanceId)
      }
    }

    data class DemoData(
      val url: String,
      val imageContentDescription: String? = null,
      val primary: String,
      val secondary: String,
      val caption: String,
    )

    val demoItems = listOf(
      DemoData(
        primary = "Seafood pasta with shrimps and tomatoes in a pan",
        secondary = "Savor the taste of the ocean with this simple, one-pan seafood pasta.",
        caption = "45 min",
        url = "https://images.unsplash.com/photo-1563379926898-05f4575a45d8",
        imageContentDescription = "image of seafood pasta",
      ),
      DemoData(
        primary = "Ramen noodles with soft boiled egg, shrimp and snow peas.",
        secondary = "Slurp up comfort with this simple ramen noodle bowl, brimming with fresh flavors",
        caption = "50 min",
        url = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624",
        imageContentDescription = "image of ramen noodles",
      ),
      DemoData(
        primary = "a platter of crackers, strawberries, and fruit",
        secondary = "Wine, cheese, crackers and berries. What more could a party ask for?",
        caption = "15 min",
        url = "https://images.unsplash.com/photo-1496412705862-e0088f16f791",
        imageContentDescription = "image of a platter of crackers, strawberries, and fruit",
      ),
      DemoData(
        primary = "Salted egg chicken rice for lunch",
        secondary = "Treat yourself to this umami-packed lunchtime delight.",
        caption = "15 min",
        url = "https://images.unsplash.com/photo-1564671165093-20688ff1fffa",
        imageContentDescription = "image of a salted egg chicken rice",
      )
    )
  }
}