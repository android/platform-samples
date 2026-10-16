/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.platform.ui.appwidgets.remote_compose

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.ui.appwidgets.R

internal val MadimiOneFontFamily = FontFamily(Font(R.font.madimi_one_regular))

internal val StoreTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
    ),
    bodyLarge = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodySmall = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

@Composable
fun StoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StoreTypography,
        content = content,
    )
}

/**
 * Companion Store activity for the Remote Compose [AndroidPetWidget].
 * Displays each active widget's configured buddy name, room preview, and collected coin balance.
 */
class StoreActivity : ComponentActivity() {

    private var activeStates by mutableStateOf<List<WidgetBuddyState>>(emptyList())
    private var selectedWidgetId by mutableStateOf<Int?>(null)

    private fun extractWidgetIdFromIntent(intent: Intent?): Int? =
        intent
            ?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        selectedWidgetId = extractWidgetIdFromIntent(intent)

        setContent {
            StoreTheme {
                StoreScreen(
                    activeStates = activeStates,
                    selectedWidgetId = selectedWidgetId,
                    onSelectBuddy = { widgetId -> selectedWidgetId = widgetId },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetIdFromIntent(intent)?.let { selectedWidgetId = it }
    }

    override fun onResume() {
        super.onResume()
        activeStates = BuddyPreferences.getActiveBuddyStates(this)
    }
}

@Composable
fun StoreScreen(
    activeStates: List<WidgetBuddyState>,
    selectedWidgetId: Int? = null,
    onSelectBuddy: (Int?) -> Unit = {},
) {
    val effectiveSelectedId = selectedWidgetId?.takeIf { id -> activeStates.any { it.widgetId == id } }
    val displayedBuddies = if (effectiveSelectedId == null) {
        activeStates
    } else {
        activeStates.filter { it.widgetId == effectiveSelectedId }
    }
    val totalCoins = activeStates.sumOf { it.coins }

    Scaffold(
        topBar = { StoreTopBar(totalCoins = totalCoins) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        ) {
            if (activeStates.size > 1) {
                BuddySelectorBar(
                    buddies = activeStates,
                    selectedWidgetId = effectiveSelectedId,
                    onSelectBuddy = onSelectBuddy,
                )
            }

            if (activeStates.isEmpty()) {
                StoreEmptyStateCard()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(displayedBuddies, key = { it.widgetId }) { state ->
                        WidgetCoinCard(state = state)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreTopBar(totalCoins: Int) {
    TopAppBar(
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.rounded_storefront),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(start = 16.dp, end = 12.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(10.dp)
                    .size(24.dp),
            )
        },
        title = {
            Column {
                Text(
                    text = "Pet Buddy Store",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Furnish & customize your buddy's room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            CreditPill(
                text = "🪙 $totalCoins",
                modifier = Modifier.padding(end = 16.dp),
            )
        },
    )
}

@Composable
fun BuddySelectorBar(
    buddies: List<WidgetBuddyState>,
    selectedWidgetId: Int?,
    onSelectBuddy: (Int?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedWidgetId == null,
                onClick = { onSelectBuddy(null) },
                label = { Text("All Buddies") },
                shape = CircleShape,
            )
        }
        items(buddies, key = { it.widgetId }) { buddy ->
            FilterChip(
                selected = selectedWidgetId == buddy.widgetId,
                onClick = { onSelectBuddy(buddy.widgetId) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = buddy.nickname)
                        Text(text = "#${buddy.widgetId}")
                    }
                },
                shape = CircleShape,
            )
        }
    }
}

@Composable
fun StoreEmptyStateCard(
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🐾",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "No active pet widgets found. Add a Pet Buddy widget to access the store!",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun WidgetCoinCard(state: WidgetBuddyState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            BuddyCardHeader(state = state)

            Spacer(modifier = Modifier.height(14.dp))

            BuddyRoomPreview(state = state)
        }
    }
}

@Composable
fun BuddyCardHeader(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${state.nickname} (#${state.widgetId})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        CreditPill(text = "${state.coins} credits 🪙")
    }
}

@Composable
fun BuddyRoomPreview(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val roomFrameBorderColor = if (isDark) Color(0xFF263540) else Color(0xFF3B4D5B)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .border(2.dp, roomFrameBorderColor, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(
                    id = BuddyPreferences.getDrawableResForEnvironment(state.environment),
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            PetAvatar(
                state = state,
                containerSize = 104.dp,
                characterSize = 88.dp,
            )
        }
    }
}

@Composable
fun PetAvatar(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
    containerSize: Dp = 104.dp,
    characterSize: Dp = 88.dp,
) {
    val bodyColorFilter = if (
        state.buddyColor != 0 && state.buddyColor != BuddyPreferences.DEFAULT_BUDDY_COLOR
    ) {
        ColorFilter.tint(Color(state.buddyColor), BlendMode.SrcIn)
    } else {
        null
    }
    val eyeMouthColorFilter = createEyeMouthColorFilter(state.eyeMouthColor)
    val bodyPainter = painterResource(
        id = BuddyPreferences.getBodyDrawableResForBuddy(state.buddyName),
    )

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .size(characterSize)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            val bodyIntrinsic = bodyPainter.intrinsicSize
            val bodyAspect = if (bodyIntrinsic.width > 0f && bodyIntrinsic.height > 0f) {
                bodyIntrinsic.width / bodyIntrinsic.height
            } else {
                1f
            }
            val boxWidth = maxWidth
            val boxHeight = maxHeight
            val boxAspect = if (boxHeight > 0.dp) boxWidth / boxHeight else 1f
            val renderedBodyWidth = if (bodyAspect >= boxAspect) boxWidth else boxHeight * bodyAspect
            val renderedBodyHeight = if (bodyAspect >= boxAspect) boxWidth / bodyAspect else boxHeight

            val eyesWidth = renderedBodyWidth * 0.65f
            val eyesOffsetY = renderedBodyHeight * (BuddyPreferences.EYES_CENTER_Y_FRACTION - 0.50f)
            val mouthWidth = renderedBodyWidth * 1.0f
            val mouthOffsetY = renderedBodyHeight * (BuddyPreferences.MOUTH_CENTER_Y_FRACTION - 0.50f)

            Image(
                painter = bodyPainter,
                contentDescription = state.nickname,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                colorFilter = bodyColorFilter,
            )
            Image(
                painter = painterResource(id = BuddyPreferences.getEyesDrawableRes(state.eyes)),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .width(eyesWidth)
                    .offset(y = eyesOffsetY),
                colorFilter = eyeMouthColorFilter,
            )
            val mouthRes = BuddyPreferences.getMouthDrawableRes(
                state.mouth,
                isGrownUp = state.isGrownUp,
            )
            if (mouthRes != null) {
                Image(
                    painter = painterResource(id = mouthRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .width(mouthWidth)
                        .offset(y = mouthOffsetY),
                    colorFilter = eyeMouthColorFilter,
                )
            }
        }
    }
}

@Composable
private fun CreditPill(text: String, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val badgeBg = if (isDark) Color(0xFF3D3112) else Color(0xFFFFF3CD)
    val badgeText = if (isDark) Color(0xFFFFD54F) else Color(0xFF7A5200)

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = badgeText,
        modifier = modifier
            .background(badgeBg, CircleShape)
            .border(1.dp, badgeText.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
