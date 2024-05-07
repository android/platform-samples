package com.example.layoutsamples.collections.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.glance.GlanceId
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.platform.ui.appwidgets.R
import com.example.layoutsamples.utils.AspectRatio
import com.example.layoutsamples.utils.AspectRatio.Companion.asDouble
import com.example.layoutsamples.collections.layout.ImageTextListItemData
import com.example.layoutsamples.utils.ImageUtils
import com.example.layoutsamples.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

/**
 * A fake in-memory implementation of repository that produces list of [ImageTextListItemData]
 */
class FakeImageTextListDataRepository {
  private val data = MutableStateFlow(listOf<ImageTextListItemData>())
  private var items = demoItems.take(MAX_ITEMS)

  /**
   * Flow of [ImageTextListItemData]s that can be listened to during a Glance session.
   */
  fun data(): Flow<List<ImageTextListItemData>> = data

  /**
   * Reloads items (which due to shuffling) helps mimic a refresh
   */
  suspend fun refresh(context: Context) {
    val showData = Random.nextInt(50) < 5

    items = if (showData) {
      items.shuffled()
    } else {
      listOf()
    }
    this.load(context)
  }

  /**
   * Loads the list of [ImageTextListItemData]s.
   */
  suspend fun load(context: Context): List<ImageTextListItemData> {
    data.value = if (items.isNotEmpty()) {
      processImagesAndBuildData(context, items)
    } else {
      listOf()
    }

    return data.value
  }

  private suspend fun processImagesAndBuildData(
    context: Context,
    items: List<DemoDataItem>,
  ): List<ImageTextListItemData> {
    val maxAllowedBytes = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val maxAllowedBytesPerImage = maxAllowedBytes / items.size
    val imageSizeLimit = ImageUtils.getMaxPossibleImageSize(
      aspectRatio = AspectRatio.Ratio1x1.asDouble(),
      memoryLimitBytes = maxAllowedBytesPerImage,
      maxImages = 1
    )

    val imageSize =
      IMAGE_SIZE.coerceAtMost(imageSizeLimit.width.coerceAtMost(imageSizeLimit.height))

    val mappedItems = runBlocking {
      items.map { item ->
        async(Dispatchers.IO) {
          var bitmap: Bitmap? = null

          val result = ImageLoader(context).execute(
            ImageRequest.Builder(context)
              .data(item.supportingImageUrl)
              .size(imageSize, imageSize)
              .networkCachePolicy(CachePolicy.ENABLED)
              .target { res: Drawable ->
                bitmap = (res as BitmapDrawable).bitmap
              }.build()
          )

          if (result is ErrorResult) {
            Log.e(TAG, "Failed to load the image:", result.throwable)
          }

          return@async ImageTextListItemData(
            key = item.key,
            title = item.title,
            supportingText = item.supportingText,
            supportingImageBitmap = bitmap,
            // In this example we are using same icon, but could potentially be different.
            trailingIconButton = R.drawable.sample_heart_icon,
            trailingIconButtonContentDescription = "Like"
          )
        }
      }.awaitAll()
    }

    return mappedItems
  }

  private data class DemoDataItem(
    val key: String,
    val title: String,
    val supportingText: String,
    val supportingImageUrl: String,
    @DrawableRes val trailingIconButton: Int? = null,
    val trailingIconButtonContentDescription: String? = null,
  )

  companion object {
    // Default size to load (that will be capped by the max allowed bytes in remote views)
    private const val IMAGE_SIZE = 200
    private const val MAX_ITEMS = 10
    private const val TAG = "FITLDR"

    private val demoItems = listOf(
      DemoDataItem(
        key = "1",
        supportingImageUrl = "https://images.unsplash.com/photo-1563379926898-05f4575a45d8",
        title = "Seafood pasta with shrimps and tomatoes in a pan",
        supportingText = "45 min",
      ),
      DemoDataItem(
        key = "2",
        title = "Oat pancakes with honey and bananas",
        supportingText = "15 min",
        supportingImageUrl = "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445",
      ),
      DemoDataItem(
        key = "3",
        title = "The ultimate barbeque platter",
        supportingText = "1 hr",
        supportingImageUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1"
      ),
      DemoDataItem(
        key = "4",
        title = "Kale olive salad with onions and pecans",
        supportingText = "15 min",
        supportingImageUrl = "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe",
      ),
      DemoDataItem(
        key = "5",
        title = "Indulging strawberry cheesecake",
        supportingText = "1 hr",
        supportingImageUrl = "https://images.unsplash.com/photo-1565958011703-44f9829ba187",
      ),
      DemoDataItem(
        key = "6",
        title = "Devil eggs with avacado toast",
        supportingText = "30 min",
        supportingImageUrl = "https://images.unsplash.com/photo-1482049016688-2d3e1b311543",
      ),
      DemoDataItem(
        key = "7",
        title = "Salmon and cucumbers!",
        supportingText = "1 hr",
        supportingImageUrl = "https://images.unsplash.com/photo-1467003909585-2f8a72700288",
      ),
      DemoDataItem(
        key = "8",
        title = "Salad: the path to a healthier, happier you",
        supportingText = "30 min",
        supportingImageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd",
      ),
    )

    private val repositories = mutableMapOf<GlanceId, FakeImageTextListDataRepository>()

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getImageTextListDataRepo(glanceId: GlanceId): FakeImageTextListDataRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsent(glanceId) { FakeImageTextListDataRepository() }
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
  }
}