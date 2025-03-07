/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.platform.ui.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.platform.ui.appwidgets.glance.layout.CanonicalLayoutActivity

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppWidgets() {
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)

    // Get a list of our app widget providers to retrieve their info
    val widgetProviders = widgetManager.getInstalledProvidersForPackage(context.packageName, null)
        .removeCanonicalLayouts()

    AppWidgetsList(widgetProviders)
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppWidgetsList(widgetProviders: List<AppWidgetProviderInfo>) {
    val context = LocalContext.current
    val widgetManager = AppWidgetManager.getInstance(context)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            InfoText(R.string.placeholder_pin_app_widget)
        }

        item {
            InfoText(
                resId = R.string.placeholder_tap_to_add_to_home,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            CanonicalLayoutsInfoCard()
        }

        // If the launcher does not support pinning request show a banner
        if (!widgetManager.isRequestPinAppWidgetSupported) {
            item {
                PinUnavailableBanner()
            }
        }

        item {
            InfoText(
                resId = R.string.placeholder_tap_to_pin,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(widgetProviders) { providerInfo ->
            WidgetInfoCard(providerInfo)
        }
    }
}

@Composable
private fun CanonicalLayoutsInfoCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = { CanonicalLayoutActivity.start(context) },
    ) {
        val preview = painterResource(id = R.drawable.cl_activity_row_hero_image)
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.canonical_layouts_info_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.canonical_layouts_info_card_description),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
            )
            Image(painter = preview, contentDescription = null)
        }
    }
}

private fun MutableList<AppWidgetProviderInfo>.removeCanonicalLayouts(): List<AppWidgetProviderInfo> {
    return this.toMutableList()
        .filter { !it.provider.className.startsWith("com.example.platform.ui.appwidgets.glance.layout") }
}


/**
 * Extension method to request the launcher to pin the given AppWidgetProviderInfo
 *
 * Note: the optional success callback to retrieve if the widget was placed might be unreliable
 * depending on the default launcher implementation. Also, it does not callback if user cancels the
 * request.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun AppWidgetProviderInfo.pin(context: Context) {
    val successCallback = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, AppWidgetPinnedReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    AppWidgetManager.getInstance(context).requestPinAppWidget(provider, null, successCallback)
}

@Composable
private fun PinUnavailableBanner() {
    Text(
        text = stringResource(
            id = R.string.placeholder_app_widget_pin_unavailable,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error),
    )
}

@Composable
private fun InfoText(
    @StringRes resId: Int,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = stringResource(id = resId),
        fontWeight = fontWeight,
        modifier = Modifier
            .fillMaxWidth(),
    )
}

/**
 * Display the app widget info from the provider.
 *
 * This class contains all the info we provide via the XML meta-data for each provider.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetInfoCard(providerInfo: AppWidgetProviderInfo) {
    val context = LocalContext.current
    val label = providerInfo.loadLabel(context.packageManager)
    val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (providerInfo.loadDescription(context) ?: "").toString()
    } else {
        "Description not available"
    }
    val preview = painterResource(id = providerInfo.previewImage)
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = {
            providerInfo.pin(context)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Image(painter = preview, contentDescription = description)
        }
    }
}
