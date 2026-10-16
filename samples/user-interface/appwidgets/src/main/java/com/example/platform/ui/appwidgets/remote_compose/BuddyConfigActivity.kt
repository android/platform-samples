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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.platform.ui.appwidgets.R

enum class ConfigStep(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
) {
    BUDDY(R.drawable.ic_nav_buddy, R.string.nav_buddy),
    NAME(R.drawable.ic_nav_name, R.string.nav_name),
    ENVIRONMENT(R.drawable.ic_nav_environment, R.string.nav_environment),
    THEME(R.drawable.ic_nav_palette, R.string.nav_palette),
}

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
                var selectedBuddy by rememberSaveable { mutableStateOf(initialState.buddyName) }
                var selectedEyes by rememberSaveable { mutableStateOf(initialState.eyes) }
                var selectedMouth by rememberSaveable { mutableStateOf(initialState.mouth) }
                var buddyNickname by rememberSaveable { mutableStateOf(initialState.nickname) }
                var selectedEnv by rememberSaveable { mutableStateOf(initialState.environment) }
                var selectedThemeColor by rememberSaveable { mutableIntStateOf(initialState.buddyColor) }
                var selectedEyeMouthColor by rememberSaveable {
                    mutableIntStateOf(initialState.eyeMouthColor)
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
                            onClick = {
                                commitSelection(
                                    buddy = selectedBuddy,
                                    nickname = buddyNickname,
                                    environment = selectedEnv,
                                    colorArgb = selectedThemeColor,
                                    eyes = selectedEyes,
                                    mouth = selectedMouth,
                                    eyeMouthColor = selectedEyeMouthColor,
                                )
                            },
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
                        selectedBuddy = selectedBuddy,
                        selectedEyes = selectedEyes,
                        selectedMouth = selectedMouth,
                        buddyNickname = buddyNickname,
                        selectedEnv = selectedEnv,
                        selectedThemeColor = selectedThemeColor,
                        selectedEyeMouthColor = selectedEyeMouthColor,
                        isGrownUp = initialState.isGrownUp,
                        onBuddySelected = { selectedBuddy = it },
                        onEyesSelected = { selectedEyes = it },
                        onMouthSelected = { selectedMouth = it },
                        onNicknameChanged = { buddyNickname = it.take(20) },
                        onEnvironmentSelected = { selectedEnv = it },
                        onThemeAndEyeColorSelected = { color, eyeColor ->
                            selectedThemeColor = color
                            selectedEyeMouthColor = eyeColor
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun commitSelection(
        buddy: String,
        nickname: String,
        environment: String,
        colorArgb: Int,
        eyes: String,
        mouth: String,
        eyeMouthColor: Int,
    ) {
        val finalNickname = nickname.trim().ifEmpty { BuddyPreferences.DEFAULT_BUDDY_NAME }
        BuddyPreferences.saveBuddyConfig(
            context = this,
            widgetId = appWidgetId,
            buddy = buddy,
            nickname = finalNickname,
            environment = environment,
            colorArgb = colorArgb,
            eyes = eyes,
            mouth = mouth,
            eyeMouthColor = eyeMouthColor,
        )

        AndroidPetWidget().onUpdate(
            this,
            AppWidgetManager.getInstance(this),
            intArrayOf(appWidgetId),
        )

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun BuddyConfigScreenContent(
    currentStep: ConfigStep,
    selectedBuddy: String,
    selectedEnv: String,
    onBuddySelected: (String) -> Unit,
    onEnvironmentSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedEyes: String = BuddyPreferences.DEFAULT_EYES,
    onEyesSelected: (String) -> Unit = {},
    selectedMouth: String = BuddyPreferences.DEFAULT_MOUTH,
    onMouthSelected: (String) -> Unit = {},
    buddyNickname: String = BuddyPreferences.DEFAULT_BUDDY_NAME,
    onNicknameChanged: (String) -> Unit = {},
    selectedThemeColor: Int = BuddyPreferences.DEFAULT_BUDDY_COLOR,
    selectedEyeMouthColor: Int = BuddyPreferences.resolveEyeMouthColor(selectedThemeColor),
    onThemeAndEyeColorSelected: (Int, Int) -> Unit = { _, _ -> },
    isGrownUp: Boolean = false,
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
            selectedBuddy = selectedBuddy,
            selectedEyes = selectedEyes,
            selectedMouth = selectedMouth,
            selectedEnv = selectedEnv,
            selectedThemeColor = selectedThemeColor,
            selectedEyeMouthColor = selectedEyeMouthColor,
            isGrownUp = isGrownUp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        )

        when (currentStep) {
            ConfigStep.BUDDY -> {
                ConfigSectionCard(titleRes = R.string.select_buddy_title) {
                    BuddySelectionRow(
                        selectedBuddy = selectedBuddy,
                        onBuddySelected = onBuddySelected,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                ConfigSectionCard(titleRes = R.string.choose_eyes_title) {
                    EyesSelectionRow(
                        selectedEyes = selectedEyes,
                        onEyesSelected = onEyesSelected,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                ConfigSectionCard(
                    titleRes = R.string.choose_mouth_title,
                    subtitleRes = R.string.choose_mouth_subtitle,
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    MouthSelectionRow(
                        selectedMouth = selectedMouth,
                        onMouthSelected = onMouthSelected,
                    )
                }
            }

            ConfigStep.NAME -> {
                ConfigSectionCard(
                    titleRes = R.string.name_buddy_title,
                    titleFontSize = 34.sp,
                    titleBottomPadding = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    NameBuddyStepContent(
                        buddyNickname = buddyNickname,
                        onNicknameChanged = onNicknameChanged,
                    )
                }
            }

            ConfigStep.ENVIRONMENT -> {
                ConfigSectionCard(
                    titleRes = R.string.select_environment_title,
                    titleFontSize = 34.sp,
                    titleBottomPadding = 12.dp,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    EnvironmentSelectionRow(
                        selectedEnv = selectedEnv,
                        onEnvironmentSelected = onEnvironmentSelected,
                    )
                }
            }

            ConfigStep.THEME -> {
                ConfigSectionCard(
                    titleRes = R.string.select_theme_title,
                    titleFontSize = 34.sp,
                    titleBottomPadding = 12.dp,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    ThemeSelectionRow(
                        selectedThemeColor = selectedThemeColor,
                        onThemeAndEyeColorSelected = onThemeAndEyeColorSelected,
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
    titleBottomPadding: androidx.compose.ui.unit.Dp = 4.dp,
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
    selectedBuddy: String,
    selectedEyes: String,
    selectedEnv: String,
    selectedThemeColor: Int,
    selectedEyeMouthColor: Int,
    modifier: Modifier = Modifier,
    selectedMouth: String = BuddyPreferences.DEFAULT_MOUTH,
    isGrownUp: Boolean = false,
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
                        id = BuddyPreferences.getDrawableResForEnvironment(selectedEnv),
                    ),
                    contentDescription = "Environment Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            PetAvatar(
                buddyName = selectedBuddy,
                eyes = selectedEyes,
                mouth = selectedMouth,
                buddyColor = selectedThemeColor,
                eyeMouthColor = selectedEyeMouthColor,
                isGrownUp = isGrownUp,
                contentDescription = stringResource(id = R.string.preview_buddy_image),
                containerSize = 160.dp,
                characterSize = 160.dp,
            )
        }
    }
}

@Composable
private fun BuddySelectionRow(
    selectedBuddy: String,
    onBuddySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyPreferences.BUDDY_OPTIONS.forEach { buddyName ->
            BuddyOption(
                buddyName = buddyName,
                isSelected = selectedBuddy == buddyName,
                onClick = { onBuddySelected(buddyName) },
            )
        }
    }
}

@Composable
private fun EyesSelectionRow(
    selectedEyes: String,
    onEyesSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyPreferences.EYE_OPTIONS.forEach { eyesStyle ->
            EyesOption(
                eyesStyle = eyesStyle,
                drawableRes = BuddyPreferences.getEyesDrawableRes(eyesStyle),
                isSelected = selectedEyes == eyesStyle,
                onClick = { onEyesSelected(eyesStyle) },
            )
        }
    }
}

@Composable
private fun EyesOption(
    eyesStyle: String,
    drawableRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(26.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = eyesStyle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = createEyeMouthColorFilter(BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR),
        )
    }
}

@Composable
private fun MouthSelectionRow(
    selectedMouth: String,
    onMouthSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyPreferences.MOUTH_OPTIONS.forEach { mouthStyle ->
            val drawableRes = BuddyPreferences.getMouthDrawableRes(mouthStyle, isGrownUp = true)
                ?: R.drawable.mouth_1
            MouthOption(
                mouthStyle = mouthStyle,
                drawableRes = drawableRes,
                isSelected = selectedMouth == mouthStyle,
                onClick = { onMouthSelected(mouthStyle) },
            )
        }
    }
}

@Composable
private fun MouthOption(
    mouthStyle: String,
    drawableRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderModifier = if (isSelected) {
        Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(26.dp),
            )
            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(26.dp))
    } else {
        Modifier
    }
    val scaleBoost = if (mouthStyle == "mouth_1") 1.4f else 1.1f
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
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

@Composable
private fun EnvironmentSelectionRow(
    selectedEnv: String,
    onEnvironmentSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyPreferences.ENVIRONMENT_OPTIONS.forEach { envName ->
            EnvOption(
                envName = envName,
                drawableRes = BuddyPreferences.getDrawableResForEnvironment(envName),
                isSelected = selectedEnv == envName,
                onClick = { onEnvironmentSelected(envName) },
            )
        }
    }
}

@Composable
private fun ThemeSelectionRow(
    selectedThemeColor: Int,
    onThemeAndEyeColorSelected: (Int, Int) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val secondaryColor = colorScheme.secondary
    val tertiaryColor = colorScheme.tertiary

    val primaryEyeAccent = if (colorScheme.onPrimaryContainer.luminance() <= colorScheme.onPrimary.luminance()) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onPrimary
    }
    val secondaryEyeAccent = if (colorScheme.onSecondaryContainer.luminance() <= colorScheme.onSecondary.luminance()) {
        colorScheme.onSecondaryContainer
    } else {
        colorScheme.onSecondary
    }
    val tertiaryEyeAccent = if (colorScheme.onTertiaryContainer.luminance() <= colorScheme.onTertiary.luminance()) {
        colorScheme.onTertiaryContainer
    } else {
        colorScheme.onTertiary
    }

    val androidGreen = Color(0xFF34A853)

    val originalBrush = Brush.sweepGradient(
        listOf(
            Color(0xFFFFA3E5), // Guy Bunny Pink
            Color(0xFF80D8FF), // Guy Bun Blue
            Color(0xFFFFD180), // Guy Wiggle Yellow
            Color(0xFFFFA3E5),
        ),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeOption(
            brush = originalBrush,
            isSelected = selectedThemeColor == 0 || selectedThemeColor == BuddyPreferences.DEFAULT_BUDDY_COLOR,
            onClick = {
                onThemeAndEyeColorSelected(
                    BuddyPreferences.DEFAULT_BUDDY_COLOR,
                    BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR,
                )
            },
        )
        ThemeOption(
            color = androidGreen,
            isSelected = selectedThemeColor == BuddyPreferences.COLOR_ANDROID_GREEN,
            onClick = {
                onThemeAndEyeColorSelected(
                    BuddyPreferences.COLOR_ANDROID_GREEN,
                    BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR,
                )
            },
        )
        listOf(
            primaryColor to primaryEyeAccent,
            secondaryColor to secondaryEyeAccent,
            tertiaryColor to tertiaryEyeAccent,
        ).forEach { (themeColor, eyeAccent) ->
            val bodyArgb = themeColor.toArgb()
            ThemeOption(
                color = themeColor,
                isSelected = selectedThemeColor == bodyArgb,
                onClick = {
                    val eyeArgb = BuddyPreferences.resolveEyeMouthColor(bodyArgb, eyeAccent.toArgb())
                    onThemeAndEyeColorSelected(bodyArgb, eyeArgb)
                },
            )
        }
    }
}

@Composable
private fun BuddyOption(
    buddyName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = BuddyPreferences.getBodyDrawableResForBuddy(buddyName)),
            contentDescription = buddyName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun EnvOption(
    envName: String,
    drawableRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(96.dp, 64.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = envName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ThemeOption(
    color: Color? = null,
    brush: Brush? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        val bgModifier = when {
            brush != null -> Modifier.background(brush)
            color != null -> Modifier.background(color)
            else -> Modifier.background(Color.Transparent)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(bgModifier),
        )
    }
}
