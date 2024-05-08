package com.example.layoutsamples.collections

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.platform.ui.appwidgets.R
import com.example.layoutsamples.collections.data.FakeImageGridDataRepository
import com.example.layoutsamples.collections.data.FakeImageGridDataRepository.Companion.getImageGridDataRepo
import com.example.layoutsamples.collections.layout.HeroStyleImageGridLayout
import com.example.layoutsamples.collections.layout.ImageGridItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A sample [GlanceAppWidget] demonstrating the [HeroStyleImageGridLayout].
 *
 * Can be configured to select different data sources.
 */
class HeroStyleImageGridAppWidget : GlanceAppWidget() {
  // Unlike the "Single" size mode, using "Exact" allows us to have better control over rendering in
  // different sizes. And, unlike the "Responsive" mode, it doesn't cause several views for each
  // supported size to be held in the widget host's memory.
  override val sizeMode: SizeMode = SizeMode.Exact
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val repo = getImageGridDataRepo(id)

    val initialItems = withContext(Dispatchers.Default) {
      repo.load(context)
    }

    provideContent {
      val items by repo.data().collectAsState(initial = initialItems)
      val coroutineScope = rememberCoroutineScope()

      GlanceTheme {
        key(LocalSize.current) {
          WidgetContent(
            items = items,
            refreshAction = {
              coroutineScope.launch {
                withContext(Dispatchers.IO) {
                  repo.refresh(context)
                }
              }
            }
          )
        }
      }
    }
  }

  @Composable
  fun WidgetContent(
    items: List<ImageGridItemData>,
    refreshAction: () -> Unit,
  ) {
    val context = LocalContext.current

    HeroStyleImageGridLayout(
      titleIconRes = R.drawable.sample_grid_icon,
      titleBarActionIconRes = R.drawable.sample_refresh_icon,
      titleBarActionIconContentDescription = context.getString(
        R.string.sample_refresh_icon_button_label
      ),
      titleBarAction = refreshAction,
      items = items
    )
  }
}

class HeroStyleImageGridAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = HeroStyleImageGridAppWidget()

  @SuppressLint("RestrictedApi")
  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    appWidgetIds.forEach {
      FakeImageGridDataRepository.cleanUp(AppWidgetId(appWidgetId = it))
    }
    super.onDeleted(context, appWidgetIds)
  }
}