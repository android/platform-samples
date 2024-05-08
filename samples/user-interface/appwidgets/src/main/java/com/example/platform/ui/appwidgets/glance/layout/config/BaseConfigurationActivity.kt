package com.example.platform.ui.appwidgets.glance.layout.config

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Use the new UX design once available.
// Should include best practices around canceling widget addition, not leading users to dead-end.
abstract class BaseConfigurationActivity(private val dataSources: List<String>) :
  ComponentActivity() {

  @OptIn(DelicateCoroutinesApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val appWidgetId: Int = intent?.extras?.getInt(
      AppWidgetManager.EXTRA_APPWIDGET_ID,
      AppWidgetManager.INVALID_APPWIDGET_ID
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    // Default to setting initial result as cancelled - e.g. in event that the activity was closed
    // by the user.
    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    setResult(Activity.RESULT_CANCELED, resultValue)

    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      Log.e(
        TAG,
        "Cannot present data source selection view, appWidgetId was missing!"
      )
      finish()
      return
    }

    val glanceId: GlanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

    setContent {
      val repo = remember { getRepo(glanceId) }
      DataSourceSelectionUI(
        dataSources = dataSources,
        onConfirm = { selectedDataSource ->
          setResultOk(appWidgetId)
          GlobalScope.launch {
            withContext(Dispatchers.Default) {
              repo.selectDataSource(applicationContext, selectedDataSource)
              updateWidget(glanceId)
            }
          }
        }
      )
    }
  }

  abstract fun getRepo(glanceId: GlanceId): BaseDataRepository
  abstract suspend fun updateWidget(glanceId: GlanceId)
  private fun setResultOk(appWidgetId: Int) {
    val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    setResult(Activity.RESULT_OK, resultValue)
    finish()
  }

  companion object {
    const val TAG = "BaseConfigurationAct"
  }
}