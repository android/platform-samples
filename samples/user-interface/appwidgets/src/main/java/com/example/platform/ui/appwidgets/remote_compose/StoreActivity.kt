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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.ui.appwidgets.R

private val MadimiOneFontFamily = FontFamily(Font(R.font.madimi_one_regular))

private val StoreTypography = Typography(
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
    bodyMedium = TextStyle(
        fontFamily = MadimiOneFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
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
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StoreTypography,
        content = content,
    )
}

/**
 * Companion Store activity for the Remote Compose [AndroidPetWidget].
 * Displays each active widget's ID and collected coin balance.
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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            StoreTopBar(totalCoins = totalCoins)

            BuddySelectorBar(
                buddies = activeStates,
                selectedWidgetId = effectiveSelectedId,
                onSelectBuddy = onSelectBuddy,
            )

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

@Composable
fun StoreTopBar(totalCoins: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_storefront),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(10.dp)
                .size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
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
        CreditPill(text = "🪙 $totalCoins")
    }
}

@Composable
fun BuddySelectorBar(
    buddies: List<WidgetBuddyState>,
    selectedWidgetId: Int?,
    onSelectBuddy: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedWidgetId == null,
            onClick = { onSelectBuddy(null) },
            label = { Text("All Buddies") },
            shape = CircleShape,
        )
        buddies.forEach { buddy ->
            FilterChip(
                selected = selectedWidgetId == buddy.widgetId,
                onClick = { onSelectBuddy(buddy.widgetId) },
                label = { Text("Android Buddy #${buddy.widgetId}") },
                shape = CircleShape,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Android Buddy: #${state.widgetId}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            CreditPill(text = "${state.coins} credits 🪙")
        }
    }
}

@Composable
private fun CreditPill(text: String) {
    val isDark = isSystemInDarkTheme()
    val badgeBg = if (isDark) Color(0xFF3D3112) else Color(0xFFFFF3CD)
    val badgeText = if (isDark) Color(0xFFFFD54F) else Color(0xFF7A5200)

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = badgeText,
        modifier = Modifier
            .background(badgeBg, CircleShape)
            .border(1.dp, badgeText.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
