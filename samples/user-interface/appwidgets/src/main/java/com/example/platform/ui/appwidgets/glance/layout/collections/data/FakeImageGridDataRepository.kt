package com.example.platform.ui.appwidgets.glance.layout.collections.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.glance.GlanceId
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio.Companion.asDouble
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridItemData
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxPossibleImageSize
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import com.example.platform.ui.appwidgets.glance.layout.computeIfAbsent as computeIfAbsentExt
/**
 * A fake in-memory implementation of repository that produces a list of
 * [ImageGridItemData].
 *
 * During the data mapping, loads resources as bitmaps, and scales them down to size within the
 * limits allowed for widgets.
 *
 */
class FakeImageGridDataRepository {
  private val data = MutableStateFlow(listOf<ImageGridItemData>())
  private var items = demoItems.take(MAX_ITEMS_PER_WIDGET)

  /**
   * Flow of list of [ImageGridItemData]s that can be listened to during a Glance session.
   */
  fun data(): Flow<List<ImageGridItemData>> = data

  suspend fun refresh(context: Context) {
    items = items.shuffled()

    this.load(context)
  }

  /**
   * Loads the [ImageGridItemData] items from the currently selected data source.
   *
   * User selected data source is set by configuration activity via [selectDataSource].
   */
  suspend fun load(context: Context): List<ImageGridItemData> {
    data.value = processImagesAndBuildData(
      context = context,
      items = items
    )

    return data.value
  }

  private suspend fun processImagesAndBuildData(
    context: Context,
    items: List<ImageGridItemBackendData>,
  ): List<ImageGridItemData> {
    val maxAllowedBytes = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val maxAllowedBytesPerImage = maxAllowedBytes / items.size
    val imageSizeLimit = getMaxPossibleImageSize(
      aspectRatio = AspectRatio.Ratio16x9.asDouble(),
      memoryLimitBytes = maxAllowedBytesPerImage,
      maxImages = 1
    )

    val width = IMAGE_SIZE.coerceAtMost(imageSizeLimit.width)
    val height = width * 9 / 16

    val mappedItems = runBlocking {
      items.map { item ->
        async(Dispatchers.IO) {
          var bitmap: Bitmap? = null

          val result = ImageLoader(context).execute(
            ImageRequest.Builder(context)
              .data(item.imageUrl)
              .size(width, height)
              .networkCachePolicy(CachePolicy.ENABLED)
              .target { res: Drawable ->
                bitmap = (res as BitmapDrawable).bitmap
              }.build()
          )

          if (result is ErrorResult) {
            Log.e(TAG, "Failed to load the image:", result.throwable)
          }

          return@async ImageGridItemData(
            key = item.key,
            title = item.title,
            supportingText = item.supportingText,
            image = bitmap,
            imageContentDescription = item.imageContentDescription
          )
        }
      }.awaitAll()
    }

    return mappedItems
  }

  private data class ImageGridItemBackendData(
    val key: String,
    val imageUrl: String,
    val imageContentDescription: String?,
    val title: String? = null,
    val supportingText: String? = null,
  )

  companion object {
    private val repositories = mutableMapOf<GlanceId, FakeImageGridDataRepository>()

    private val demoItems = listOf(
      ImageGridItemBackendData(
        key = "1",
        imageUrl = "https://images.unsplash.com/photo-1444464666168-49d633b86797",
        imageContentDescription = "Perched blue and orange bird",
        title = "Perched blue and orange bird",
        supportingText = "2000 views"
      ),
      ImageGridItemBackendData(
        key = "2",
        imageUrl = "https://images.unsplash.com/photo-1500349812227-3264f5f54181",
        imageContentDescription = "Orange Lacewing butterfly",
        title = "Orange Lacewing butterfly",
        supportingText = "1900 views"
      ),
      ImageGridItemBackendData(
        key = "3",
        imageUrl = "https://images.unsplash.com/photo-1578326457399-3b34dbbf23b8",
        imageContentDescription = "Zebra",
        title = "Zebra in African safari",
        supportingText = "7000 views"
      ),
      ImageGridItemBackendData(
        key = "4",
        imageUrl = "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7",
        imageContentDescription = "Panda",
        title = "The hungry panda",
        supportingText = "1100 views"
      ),
      ImageGridItemBackendData(
        key = "5",
        imageUrl = "https://images.unsplash.com/photo-1585256262155-c044dc6ad38e",
        imageContentDescription = "Humming bird",
        title = "Beautiful humming bird",
        supportingText = "700 views"
      ),
      ImageGridItemBackendData(
        key = "6",
        imageUrl = "https://images.unsplash.com/photo-1497752531616-c3afd9760a11",
        imageContentDescription = "Racoon",
        title = "Racoon on lawn grass",
        supportingText = "1500 views"
      ),
      ImageGridItemBackendData(
        key = "7",
        imageUrl = "https://images.unsplash.com/photo-1425082661705-1834bfd09dca",
        imageContentDescription = "Brown hamster",
        title = "Brown hamster",
        supportingText = "800 views"
      ),
      ImageGridItemBackendData(
        key = "8",
        imageUrl = "https://images.unsplash.com/reserve/RFDKkrvXSHqBaWMMl4W5_Heavy_company",
        imageContentDescription = "Elephant in afternoon sun",
        title = "Elephant in afternoon sun",
        supportingText = "1200 views"
      )
    )

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getImageGridDataRepo(glanceId: GlanceId): FakeImageGridDataRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsentExt(glanceId) { FakeImageGridDataRepository() }!!
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

    // Capped at 8 to limit amount of memory consumed by bitmaps
    const val MAX_ITEMS_PER_WIDGET = 8
    const val IMAGE_SIZE = 200
    const val TAG = "FIGDR"
  }
}