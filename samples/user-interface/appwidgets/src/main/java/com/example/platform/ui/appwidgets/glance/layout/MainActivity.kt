package com.example.layoutsamples

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.layoutsamples.ui.theme.LayoutSamplesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val TAG = "layoutsamples"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ourWidgets = ourWidgets()

        setContent {
            val scope = rememberCoroutineScope()
            LayoutSamplesTheme {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    item {
                        AppDescription()
                    }
                    items(ourWidgets) { providerInfo: AppWidgetProviderInfo ->
                        WidgetRow(
                            providerInfo = providerInfo,
                            onClick = { requestPin(providerInfo, this@MainActivity, scope) })
                    }
                }
            }
        }
    }
}

private fun Context.ourWidgets(): List<AppWidgetProviderInfo> {
    val appWidgetManager = AppWidgetManager.getInstance(this)
    return appWidgetManager.installedProviders
        .filter { providerInfo: AppWidgetProviderInfo -> providerInfo.provider.packageName == packageName }
}

@Composable
private fun AppDescription() {
    Column {
        Text(
            "Layout Samples",
            style = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
        )
        Text(
            "Layout Samples demonstrate common widget design patterns. Tap on a layout from" +
                    " the list to pin it to your home screen."
        )
    }

}

@Composable
private fun WidgetRow(
    providerInfo: AppWidgetProviderInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val name = providerInfo.loadLabel(context.packageManager)
    
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(4.dp),
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Text(
                text = name,
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = TextStyle(fontWeight = FontWeight.W500 )
            )
        }
    }
}

private fun requestPin(
    providerInfo: AppWidgetProviderInfo,
    context: Context,
    coroutineScope: CoroutineScope
) {
    val glanceManager = GlanceAppWidgetManager(context)
    val receiverClass = Class.forName(providerInfo.provider.className)
    val receiver = receiverClass.getDeclaredConstructor()
        .newInstance() as GlanceAppWidgetReceiver

    coroutineScope.launch {
        glanceManager.requestPinGlanceAppWidget(receiver::class.java)
    }
}