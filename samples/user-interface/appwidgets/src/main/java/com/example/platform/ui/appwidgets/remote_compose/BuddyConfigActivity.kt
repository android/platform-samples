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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.platform.ui.appwidgets.R

enum class ConfigStep(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    @StringRes val titleRes: Int,
) {
    BUDDY(R.drawable.ic_nav_buddy, R.string.nav_buddy, R.string.select_buddy_title),
    NAME(R.drawable.ic_nav_name, R.string.nav_name, R.string.name_buddy_title),
    ENVIRONMENT(R.drawable.ic_nav_environment, R.string.nav_environment, R.string.select_environment_title),
    THEME(R.drawable.ic_nav_palette, R.string.nav_palette, R.string.select_theme_title),
}

private val WidgetBuddyStateSaver = listSaver<WidgetBuddyState, Any>(
    save = {
        listOf(
            it.widgetId,
            it.coins,
            it.buddyName,
            it.nickname,
            it.eyes,
            it.mouth,
            it.environment,
            it.buddyColor,
            it.eyeMouthColor,
            it.isGrownUp,
        )
    },
    restore = {
        WidgetBuddyState(
            widgetId = it[0] as Int,
            coins = it[1] as Int,
            buddyName = it[2] as String,
            nickname = it[3] as String,
            eyes = it[4] as String,
            mouth = it[5] as String,
            environment = it[6] as String,
            buddyColor = it[7] as Int,
            eyeMouthColor = it[8] as Int,
            isGrownUp = it[9] as Boolean,
        )
    },
)

/**
 * Configuration activity for [AndroidPetWidget], allowing the user to customize the pet's
 * body, eyes, mouth, nickname, room environment, and theme colors before placing the widget.
 */
class BuddyConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val initialState = BuddyPreferences.getWidgetBuddyState(this, appWidgetId)

        setContent {
            StoreTheme(darkTheme = false) {
                var currentStep by rememberSaveable { mutableStateOf(ConfigStep.BUDDY) }
                var state by rememberSaveable(stateSaver = WidgetBuddyStateSaver) {
                    mutableStateOf(initialState)
                }

                val itemColors = NavigationSuiteDefaults.itemColors(
                    navigationBarItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF1B1C17),
                        indicatorColor = Color(0xFF50524A),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                    ),
                    navigationRailItemColors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF1B1C17),
                        indicatorColor = Color(0xFF50524A),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                    ),
                )

                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        ConfigStep.entries.forEach { step ->
                            item(
                                selected = currentStep == step,
                                onClick = { currentStep = step },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = step.iconRes),
                                        contentDescription = stringResource(id = step.labelRes),
                                        modifier = Modifier.size(32.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = step.labelRes),
                                        fontFamily = MadimiOneFontFamily,
                                    )
                                },
                                colors = itemColors,
                            )
                        }
                        item(
                            selected = false,
                            onClick = { commitSelection(state) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                                    contentDescription = stringResource(id = R.string.save_to_home_screen),
                                    modifier = Modifier.size(32.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.nav_place),
                                    fontFamily = MadimiOneFontFamily,
                                )
                            },
                            colors = itemColors,
                        )
                    },
                    containerColor = colorResource(id = R.color.config_bg),
                    navigationSuiteColors = NavigationSuiteDefaults.colors(
                        navigationBarContainerColor = Color(0xFFEEF3E7),
                        navigationRailContainerColor = Color(0xFFEEF3E7),
                    ),
                ) {
                    BuddyConfigScreenContent(
                        currentStep = currentStep,
                        state = state,
                        onStateChange = { state = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun commitSelection(state: WidgetBuddyState) {
        BuddyPreferences.saveBuddyConfig(this, state)
        AndroidPetWidget().onUpdate(
            this,
            AppWidgetManager.getInstance(this),
            intArrayOf(appWidgetId),
        )
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }
}

@Composable
fun BuddyConfigScreenContent(
    currentStep: ConfigStep,
    state: WidgetBuddyState,
    onStateChange: (WidgetBuddyState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BuddyConfigHeroPreviewCard(
            currentStep = currentStep,
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        )

        if (currentStep == ConfigStep.BUDDY) {
            ConfigSectionCard(titleRes = R.string.select_buddy_title) {
                BuddySelectionRow(
                    selectedBuddy = state.buddyName,
                    onBuddySelected = { onStateChange(state.copy(buddyName = it)) },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ConfigSectionCard(titleRes = R.string.choose_eyes_title) {
                EyesSelectionRow(
                    selectedEyes = state.eyes,
                    onEyesSelected = { onStateChange(state.copy(eyes = it)) },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ConfigSectionCard(
                titleRes = R.string.choose_mouth_title,
                subtitleRes = R.string.choose_mouth_subtitle,
                modifier = Modifier.padding(bottom = 2.dp),
            ) {
                MouthSelectionRow(
                    selectedMouth = state.mouth,
                    onMouthSelected = { onStateChange(state.copy(mouth = it)) },
                )
            }
        } else {
            ConfigSectionCard(
                titleRes = currentStep.titleRes,
                titleFontSize = 34.sp,
                titleBottomPadding = 12.dp,
                shape = RoundedCornerShape(if (currentStep == ConfigStep.NAME) 24.dp else 48.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                when (currentStep) {
                    ConfigStep.NAME -> NameBuddyStepContent(
                        buddyNickname = state.nickname,
                        onNicknameChanged = { onStateChange(state.copy(nickname = it.take(20))) },
                    )
                    ConfigStep.ENVIRONMENT -> EnvironmentSelectionRow(
                        selectedEnv = state.environment,
                        onEnvironmentSelected = { onStateChange(state.copy(environment = it)) },
                    )
                    else -> ThemeSelectionRow(
                        selectedThemeColor = state.buddyColor,
                        onThemeAndEyeColorSelected = { color, eyeColor ->
                            onStateChange(state.copy(buddyColor = color, eyeMouthColor = eyeColor))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigSectionCard(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    @StringRes subtitleRes: Int? = null,
    titleFontSize: TextUnit = 22.sp,
    titleBottomPadding: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(48.dp),
    content: @Composable () -> Unit,
) {
    val titleColor = colorResource(id = R.color.title_text_color)

    Text(
        text = stringResource(id = titleRes),
        style = TextStyle(
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = MadimiOneFontFamily,
            color = titleColor,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (subtitleRes == null) titleBottomPadding else 0.dp),
    )

    if (subtitleRes != null) {
        Text(
            text = stringResource(id = subtitleRes),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = MadimiOneFontFamily,
                color = titleColor.copy(alpha = 0.75f),
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = titleBottomPadding),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = shape,
        color = colorResource(id = R.color.selection_card_bg),
        shadowElevation = 4.dp,
        content = content,
    )
}

@Composable
fun NameBuddyStepContent(
    buddyNickname: String,
    onNicknameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OutlinedTextField(
            value = buddyNickname,
            onValueChange = onNicknameChanged,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = MadimiOneFontFamily,
                fontSize = 18.sp,
            ),
            label = {
                Text(
                    text = stringResource(id = R.string.name_buddy_label),
                    fontFamily = MadimiOneFontFamily,
                )
            },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.name_buddy_placeholder),
                    fontFamily = MadimiOneFontFamily,
                )
            },
            singleLine = true,
            trailingIcon = {
                if (buddyNickname.isNotEmpty()) {
                    IconButton(onClick = { onNicknameChanged("") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clear),
                            contentDescription = stringResource(id = R.string.clear_name),
                        )
                    }
                }
            },
            supportingText = {
                Text(
                    text = "${buddyNickname.length} / 20",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = MadimiOneFontFamily,
                )
            },
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun BuddyConfigHeroPreviewCard(
    currentStep: ConfigStep,
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (currentStep == ConfigStep.BUDDY || currentStep == ConfigStep.NAME) {
                Image(
                    painter = painterResource(id = R.drawable.expressive_shape_2),
                    contentDescription = "Shape Background",
                    modifier = Modifier.size(260.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Image(
                    painter = painterResource(
                        id = BuddyPreferences.getDrawableResForEnvironment(state.environment),
                    ),
                    contentDescription = "Environment Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            PetAvatar(
                state = state,
                containerSize = 160.dp,
                characterSize = 160.dp,
                contentDescription = stringResource(id = R.string.preview_buddy_image),
            )
        }
    }
}

@Composable
private fun OptionRow(
    verticalPadding: Dp = 6.dp,
    horizontalPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding, horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SelectableOptionBox(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    selectedBackgroundColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val selectionModifier = if (isSelected) {
        Modifier
            .background(selectedBackgroundColor, shape)
            .border(3.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .then(selectionModifier)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun BuddySelectionRow(
    selectedBuddy: String,
    onBuddySelected: (String) -> Unit,
) {
    OptionRow {
        BuddyPreferences.BUDDY_OPTIONS.forEach { buddyName ->
            SelectableOptionBox(
                isSelected = selectedBuddy == buddyName,
                onClick = { onBuddySelected(buddyName) },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Image(
                    painter = painterResource(id = BuddyPreferences.getBodyDrawableResForBuddy(buddyName)),
                    contentDescription = buddyName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun EyesSelectionRow(
    selectedEyes: String,
    onEyesSelected: (String) -> Unit,
) {
    OptionRow(horizontalPadding = 12.dp) {
        BuddyPreferences.EYE_OPTIONS.forEach { eyesStyle ->
            SelectableOptionBox(
                isSelected = selectedEyes == eyesStyle,
                onClick = { onEyesSelected(eyesStyle) },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Image(
                    painter = painterResource(id = BuddyPreferences.getEyesDrawableRes(eyesStyle)),
                    contentDescription = eyesStyle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = createEyeMouthColorFilter(BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR),
                )
            }
        }
    }
}

@Composable
private fun MouthSelectionRow(
    selectedMouth: String,
    onMouthSelected: (String) -> Unit,
) {
    OptionRow {
        BuddyPreferences.MOUTH_OPTIONS.forEach { mouthStyle ->
            val drawableRes = BuddyPreferences.getMouthDrawableRes(mouthStyle, isGrownUp = true)
                ?: R.drawable.mouth_1
            val scaleBoost = if (mouthStyle == "mouth_1") 1.4f else 1.1f
            SelectableOptionBox(
                isSelected = selectedMouth == mouthStyle,
                onClick = { onMouthSelected(mouthStyle) },
                modifier = Modifier.size(width = 72.dp, height = 52.dp),
                shape = RoundedCornerShape(26.dp),
                selectedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = mouthStyle,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scaleBoost
                            scaleY = scaleBoost
                        },
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.Center,
                    colorFilter = createEyeMouthColorFilter(BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR),
                )
            }
        }
    }
}

@Composable
private fun EnvironmentSelectionRow(
    selectedEnv: String,
    onEnvironmentSelected: (String) -> Unit,
) {
    OptionRow(verticalPadding = 12.dp) {
        BuddyPreferences.ENVIRONMENT_OPTIONS.forEach { envName ->
            SelectableOptionBox(
                isSelected = selectedEnv == envName,
                onClick = { onEnvironmentSelected(envName) },
                modifier = Modifier.size(96.dp, 64.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                Image(
                    painter = painterResource(id = BuddyPreferences.getDrawableResForEnvironment(envName)),
                    contentDescription = envName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectionRow(
    selectedThemeColor: Int,
    onThemeAndEyeColorSelected: (Int, Int) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    fun darkerColor(a: Color, b: Color): Color = if (a.luminance() <= b.luminance()) a else b

    val originalBrush = Brush.sweepGradient(
        listOf(
            Color(0xFFFFA3E5), // Guy Bunny Pink
            Color(0xFF80D8FF), // Guy Bun Blue
            Color(0xFFFFD180), // Guy Wiggle Yellow
            Color(0xFFFFA3E5),
        ),
    )

    OptionRow(verticalPadding = 12.dp) {
        SelectableOptionBox(
            isSelected = selectedThemeColor == 0 || selectedThemeColor == BuddyPreferences.DEFAULT_BUDDY_COLOR,
            onClick = {
                onThemeAndEyeColorSelected(
                    BuddyPreferences.DEFAULT_BUDDY_COLOR,
                    BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR,
                )
            },
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(originalBrush),
            )
        }

        val swatchOptions = listOf(
            Color(0xFF34A853) to BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR,
            colorScheme.primary to darkerColor(colorScheme.onPrimaryContainer, colorScheme.onPrimary).toArgb(),
            colorScheme.secondary to darkerColor(colorScheme.onSecondaryContainer, colorScheme.onSecondary).toArgb(),
            colorScheme.tertiary to darkerColor(colorScheme.onTertiaryContainer, colorScheme.onTertiary).toArgb(),
        )

        swatchOptions.forEach { (themeColor, eyeAccentArgb) ->
            val bodyArgb = themeColor.toArgb()
            SelectableOptionBox(
                isSelected = selectedThemeColor == bodyArgb,
                onClick = {
                    val eyeArgb = BuddyPreferences.resolveEyeMouthColor(bodyArgb, eyeAccentArgb)
                    onThemeAndEyeColorSelected(bodyArgb, eyeArgb)
                },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(themeColor),
                )
            }
        }
    }
}
