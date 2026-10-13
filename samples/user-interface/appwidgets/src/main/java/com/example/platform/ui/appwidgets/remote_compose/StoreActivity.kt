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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.ui.appwidgets.R

val MadimiOneFontFamily = FontFamily(
    Font(R.font.madimi_one_regular, FontWeight.Normal),
    Font(R.font.madimi_one_regular, FontWeight.Medium),
    Font(R.font.madimi_one_regular, FontWeight.SemiBold),
    Font(R.font.madimi_one_regular, FontWeight.Bold),
    Font(R.font.madimi_one_regular, FontWeight.ExtraBold),
)

/**
 * Brand color tokens for the modern, playful Pet Buddy Store aesthetic.
 */
object StoreColors {
    // Light Mode Tokens
    val BackgroundLight = Color(0xFFF5F7F0)
    val SurfaceCardLight = Color(0xFFFFFFFF)
    val RoomFrameBgLight = Color(0xFF3B4D5B)
    val PrimaryLight = Color(0xFF2E6B4E)
    val SecondaryContainerLight = Color(0xFFE8F3EC)
    val OwnedContainerLight = Color(0xFFEAF7EE)
    val OwnedBorderLight = Color(0xFF4CAF50)
    val CoinBadgeBgLight = Color(0xFFFFF3CD)
    val CoinBadgeTextLight = Color(0xFF7A5200)
    val DiscoAccentBgLight = Color(0xFF4A154B)
    val DiscoAccentBorder = Color(0xFFFFD54F)
    val DisabledContainerLight = Color(0xFFF0F1EE)
    val TitleTextLight = Color(0xFF263329)
    val SubtitleTextLight = Color(0xFF526056)

    // Dark Mode Tokens
    val BackgroundDark = Color(0xFF141815)
    val SurfaceCardDark = Color(0xFF1E2420)
    val RoomFrameBgDark = Color(0xFF263540)
    val PrimaryDark = Color(0xFF81C784)
    val SecondaryContainerDark = Color(0xFF25382E)
    val OwnedContainerDark = Color(0xFF1F3528)
    val OwnedBorderDark = Color(0xFF66BB6A)
    val CoinBadgeBgDark = Color(0xFF3D3112)
    val CoinBadgeTextDark = Color(0xFFFFD54F)
    val DiscoAccentBgDark = Color(0xFF5C1D5E)
    val DisabledContainerDark = Color(0xFF252826)
    val TitleTextDark = Color(0xFFE8F0EA)
    val SubtitleTextDark = Color(0xFFA6B5AA)
}

@Immutable
data class StorePalette(
    val background: Color,
    val surfaceCard: Color,
    val roomFrameBg: Color,
    val primary: Color,
    val secondaryContainer: Color,
    val ownedContainer: Color,
    val ownedBorder: Color,
    val coinBadgeBg: Color,
    val coinBadgeText: Color,
    val discoAccentBg: Color,
    val discoAccentBorder: Color,
    val disabledContainer: Color,
    val titleText: Color,
    val subtitleText: Color,
)

private val LightStorePalette = StorePalette(
    background = StoreColors.BackgroundLight,
    surfaceCard = StoreColors.SurfaceCardLight,
    roomFrameBg = StoreColors.RoomFrameBgLight,
    primary = StoreColors.PrimaryLight,
    secondaryContainer = StoreColors.SecondaryContainerLight,
    ownedContainer = StoreColors.OwnedContainerLight,
    ownedBorder = StoreColors.OwnedBorderLight,
    coinBadgeBg = StoreColors.CoinBadgeBgLight,
    coinBadgeText = StoreColors.CoinBadgeTextLight,
    discoAccentBg = StoreColors.DiscoAccentBgLight,
    discoAccentBorder = StoreColors.DiscoAccentBorder,
    disabledContainer = StoreColors.DisabledContainerLight,
    titleText = StoreColors.TitleTextLight,
    subtitleText = StoreColors.SubtitleTextLight,
)

private val DarkStorePalette = StorePalette(
    background = StoreColors.BackgroundDark,
    surfaceCard = StoreColors.SurfaceCardDark,
    roomFrameBg = StoreColors.RoomFrameBgDark,
    primary = StoreColors.PrimaryDark,
    secondaryContainer = StoreColors.SecondaryContainerDark,
    ownedContainer = StoreColors.OwnedContainerDark,
    ownedBorder = StoreColors.OwnedBorderDark,
    coinBadgeBg = StoreColors.CoinBadgeBgDark,
    coinBadgeText = StoreColors.CoinBadgeTextDark,
    discoAccentBg = StoreColors.DiscoAccentBgDark,
    discoAccentBorder = StoreColors.DiscoAccentBorder,
    disabledContainer = StoreColors.DisabledContainerDark,
    titleText = StoreColors.TitleTextDark,
    subtitleText = StoreColors.SubtitleTextDark,
)

val LocalStorePalette = staticCompositionLocalOf { LightStorePalette }

private val StoreLightColorScheme = lightColorScheme(
    primary = StoreColors.PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = StoreColors.SecondaryContainerLight,
    onPrimaryContainer = StoreColors.TitleTextLight,
    secondary = StoreColors.RoomFrameBgLight,
    onSecondary = Color.White,
    secondaryContainer = StoreColors.SecondaryContainerLight,
    onSecondaryContainer = StoreColors.TitleTextLight,
    background = StoreColors.BackgroundLight,
    onBackground = StoreColors.TitleTextLight,
    surface = StoreColors.SurfaceCardLight,
    onSurface = StoreColors.TitleTextLight,
    surfaceVariant = StoreColors.SecondaryContainerLight,
    onSurfaceVariant = StoreColors.SubtitleTextLight,
)

private val StoreDarkColorScheme = darkColorScheme(
    primary = StoreColors.PrimaryDark,
    onPrimary = Color(0xFF102819),
    primaryContainer = StoreColors.SecondaryContainerDark,
    onPrimaryContainer = StoreColors.TitleTextDark,
    secondary = StoreColors.RoomFrameBgDark,
    onSecondary = Color.White,
    secondaryContainer = StoreColors.SecondaryContainerDark,
    onSecondaryContainer = StoreColors.TitleTextDark,
    background = StoreColors.BackgroundDark,
    onBackground = StoreColors.TitleTextDark,
    surface = StoreColors.SurfaceCardDark,
    onSurface = StoreColors.TitleTextDark,
    surfaceVariant = StoreColors.SecondaryContainerDark,
    onSurfaceVariant = StoreColors.SubtitleTextDark,
)

val StoreTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
    ),
    displayMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
    ),
    displaySmall = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineLarge = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 26.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 21.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleSmall = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
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
    labelMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun StoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) StoreDarkColorScheme else StoreLightColorScheme
    val palette = if (darkTheme) DarkStorePalette else LightStorePalette

    CompositionLocalProvider(LocalStorePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StoreTypography,
            content = content,
        )
    }
}

/**
 * Companion Store activity for the Remote Compose [AndroidPetWidget].
 * Displays each active widget's ID and collected coin balance.
 */
class StoreActivity : ComponentActivity() {

    private var activeStates by mutableStateOf<List<WidgetBuddyState>>(emptyList())
    private var selectedWidgetId by mutableStateOf<Int?>(null)

    private fun extractWidgetIdFromIntent(intent: Intent?): Int? {
        if (intent == null) return null
        val standardId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (standardId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            return standardId
        }
        val fallbackId = intent.getIntExtra(
            "appWidgetId",
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        return if (fallbackId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            fallbackId
        } else {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        activeStates = BuddyPreferences.getActiveBuddyStates(this)
        selectedWidgetId = extractWidgetIdFromIntent(intent)

        setContent {
            StoreTheme {
                StoreScreen(
                    activeStates = activeStates,
                    selectedWidgetId = selectedWidgetId,
                    onSelectBuddy = { widgetId ->
                        selectedWidgetId = widgetId
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incomingWidgetId = extractWidgetIdFromIntent(intent)
        if (incomingWidgetId != null) {
            selectedWidgetId = incomingWidgetId
        }
        activeStates = BuddyPreferences.getActiveBuddyStates(this)
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
    modifier: Modifier = Modifier,
) {
    val palette = LocalStorePalette.current
    val effectiveSelectedId = selectedWidgetId?.takeIf { id -> activeStates.any { it.widgetId == id } }
    val displayedBuddies = if (effectiveSelectedId == null) {
        activeStates
    } else {
        activeStates.filter { it.widgetId == effectiveSelectedId }
    }
    val totalCoins = activeStates.sumOf { it.coins }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                )
                .consumeWindowInsets(innerPadding),
        ) {
            StoreTopBar(totalCoins = totalCoins)

            if (activeStates.isNotEmpty()) {
                BuddySelectorBar(
                    buddies = activeStates,
                    selectedWidgetId = effectiveSelectedId,
                    onSelectBuddy = onSelectBuddy,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            if (activeStates.isEmpty()) {
                StoreEmptyStateCard()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("store_buddy_list"),
                    contentPadding = PaddingValues(
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
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

@Composable
fun StoreTopBar(
    totalCoins: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStorePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("store_top_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Surface(
                shape = CircleShape,
                color = palette.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_storefront),
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Pet Buddy Store",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.titleText,
                )
                Text(
                    text = "Furnish & customize your buddy's room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.subtitleText,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = palette.coinBadgeBg,
            border = BorderStroke(1.dp, palette.coinBadgeText.copy(alpha = 0.25f)),
            modifier = Modifier.testTag("store_header_coin_badge"),
        ) {
            Text(
                text = "🪙 $totalCoins",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.coinBadgeText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun BuddySelectorBar(
    buddies: List<WidgetBuddyState>,
    selectedWidgetId: Int?,
    onSelectBuddy: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStorePalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("buddy_selector_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val allSelected = selectedWidgetId == null
        Surface(
            shape = RoundedCornerShape(50),
            color = if (allSelected) palette.primary else palette.surfaceCard,
            border = BorderStroke(
                width = 1.dp,
                color = if (allSelected) palette.primary else palette.subtitleText.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onSelectBuddy(null) }
                .semantics { selected = allSelected }
                .testTag("buddy_selector_all"),
        ) {
            Text(
                text = "All Buddies",
                style = MaterialTheme.typography.labelLarge,
                color = if (allSelected) Color.White else palette.titleText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        buddies.forEach { buddy ->
            val isSelected = selectedWidgetId == buddy.widgetId
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) palette.primary else palette.surfaceCard,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) palette.primary else palette.subtitleText.copy(alpha = 0.3f),
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectBuddy(buddy.widgetId) }
                    .semantics { selected = isSelected }
                    .testTag("buddy_selector_chip_${buddy.widgetId}"),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = if (isSelected) Color.White else palette.titleText,
                            ),
                        ) {
                            append("Android Buddy ")
                        }
                        withStyle(
                            SpanStyle(
                                color = if (isSelected) {
                                    Color.White.copy(alpha = 0.85f)
                                } else {
                                    palette.subtitleText
                                },
                            ),
                        ) {
                            append("#${buddy.widgetId}")
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun StoreEmptyStateCard(
    modifier: Modifier = Modifier,
) {
    val palette = LocalStorePalette.current
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("store_empty_state_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = palette.surfaceCard),
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
                color = palette.titleText,
            )
        }
    }
}

@Composable
fun WidgetCoinCard(
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
) {
    val palette = LocalStorePalette.current
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("store_buddy_card_${state.widgetId}"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = palette.surfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Android Buddy: #${state.widgetId}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.titleText,
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.coinBadgeBg,
                    border = BorderStroke(1.dp, palette.coinBadgeText.copy(alpha = 0.25f)),
                ) {
                    Text(
                        text = "${state.coins} credits 🪙",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.coinBadgeText,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("text_store_credits_${state.widgetId}"),
                    )
                }
            }
        }
    }
}
