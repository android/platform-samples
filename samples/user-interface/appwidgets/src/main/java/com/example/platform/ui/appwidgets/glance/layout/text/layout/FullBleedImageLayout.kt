/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.appwidgets.glance.layout.text.layout

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridItemData
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.NoDataContent
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.FontUtils
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A beautiful canonical layout featuring an edge-to-edge full bleed gallery with snapping
 * vertical scroll support, reusing the unified [ImageGridItemData] schema.
 *
 * Each featured item displays an edge-to-edge landscape background photo with overlaid
 * title and caption details that auto-scale to fit the current widget size.
 */
@RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
@Composable
fun FullBleedImageLayout(
    data: List<ImageGridItemData>? = null,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
    ) {
        if (data.isNullOrEmpty()) {
            val context = LocalContext.current
            NoDataContent(
                noDataText = context.getString(R.string.sample_no_data_text),
                noDataIconRes = R.drawable.sample_no_data_icon,
                actionButtonText = context.getString(R.string.sample_learn_more_button_text),
                actionButtonIcon = R.drawable.sample_info_icon,
                actionButtonOnClick = actionStartDemoActivity("on-click of info button in no data view")
            )
        } else {
            val limitedData = data.take(5)
            // LazyColumn with SnapScroll mode enabled
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize(),
                verticalScrollMode = VerticalScrollMode.SnapScroll
            ) {
                items(limitedData) { item ->
                    GalleryItemCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun GalleryItemCard(item: ImageGridItemData) {
    val size = LocalSize.current
    val itemTitle = item.title ?: ""

    Box(
        modifier = GlanceModifier
            .width(size.width)
            .height(size.height),
        contentAlignment = Alignment.BottomStart
    ) {
        // Full bleed background image
        val imageProvider = if (item.image != null) {
            ImageProvider(item.image)
        } else {
            ImageProvider(R.drawable.sample_placeholder_image)
        }

        Image(
            provider = imageProvider,
            contentDescription = item.imageContentDescription ?: itemTitle,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize()
        )

        // Available space for texts within the card boundaries (with 16.dp padding top/bottom and start/end)
        val availableWidth = size.width - 32.dp
        val availableHeight = size.height - 32.dp

        // Distribute available height proportionally (65% for Title, 35% for Caption)
        val titleAvailableHeight = availableHeight * 0.65f
        val captionAvailableHeight = availableHeight * 0.35f

        // Calculate Caption size dynamically using FontUtils
        val captionText = item.supportingText ?: ""
        val (captionFontSize, captionMaxLines) = if (captionText.isNotEmpty()) {
            FontUtils.calculateFontSizeAndMaxLines(
                context = LocalContext.current,
                text = captionText,
                availableWidth = availableWidth,
                availableHeight = captionAvailableHeight,
                minFontSize = 10.sp,
                maxFontSize = 16.sp // Best available representing 36.sp caption
            )
        } else {
            12.sp to 1
        }

        // Calculate Title size dynamically using FontUtils
        val (titleFontSize, titleMaxLines) = if (itemTitle.isNotEmpty()) {
            FontUtils.calculateFontSizeAndMaxLines(
                context = LocalContext.current,
                text = itemTitle,
                availableWidth = availableWidth,
                availableHeight = titleAvailableHeight,
                minFontSize = 10.sp,
                maxFontSize = 28.sp // Best available representing 45.sp title
            )
        } else {
            16.sp to 1
        }

        // Overlay layout containing Title and Caption, without gradient scrim.
        // We add generous padding so text doesn't touch the widget edges.
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Caption styled in explicit white color
            if (captionText.isNotEmpty()) {
                Text(
                    text = captionText,
                    maxLines = captionMaxLines,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Normal,
                        fontSize = captionFontSize
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                )
                if (itemTitle.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }

            // Dynamic title sizing using FontUtils auto-scaling
            if (itemTitle.isNotEmpty()) {
                Text(
                    text = itemTitle,
                    maxLines = titleMaxLines,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFontSize
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
@SmallWidgetPreview
@MediumWidgetPreview
@Composable
private fun FullBleedImageLayoutPreview() {
    val previewItems = listOf(
        ImageGridItemData(
            key = "0",
            title = "Yosemite Valley under clear blue skies",
            supportingText = "Yosemite National Park, California",
            image = null,
            imageContentDescription = null
        ),
        ImageGridItemData(
            key = "1",
            title = "Mystical forest lake reflection at sunrise",
            supportingText = "Avenue of the Giants, Oregon",
            image = null,
            imageContentDescription = null
        )
    )

    FullBleedImageLayout(
        data = previewItems
    )
}
