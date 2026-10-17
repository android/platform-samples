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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.platform.ui.appwidgets.R

private const val MAX_NICKNAME_LENGTH = 20

private enum class ConfigStep(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    @StringRes val titleRes: Int,
) {
    BUDDY(R.drawable.ic_nav_buddy, R.string.nav_buddy, R.string.select_buddy_title),
    NAME(R.drawable.ic_nav_name, R.string.nav_name, R.string.name_buddy_title),
    ENVIRONMENT(R.drawable.ic_nav_environment, R.string.nav_environment, R.string.select_environment_title),
    THEME(R.drawable.ic_nav_palette, R.string.nav_palette, R.string.select_theme_title),
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

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        val initialState = BuddyPreferences.getWidgetBuddyState(this, appWidgetId)

        setContent {
            StoreTheme {
                var currentStep by rememberSaveable { mutableStateOf(ConfigStep.BUDDY) }
                var state by rememberSaveable(stateSaver = WidgetBuddyState.Saver) {
                    mutableStateOf(initialState)
                }

                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        ConfigStep.entries.forEach { step ->
                            configNavItem(
                                selected = currentStep == step,
                                onClick = { currentStep = step },
                                iconRes = step.iconRes,
                                labelRes = step.labelRes,
                            )
                        }
                        configNavItem(
                            selected = false,
                            onClick = { commitSelection(state) },
                            iconRes = R.drawable.ic_arrow_forward,
                            labelRes = R.string.nav_place,
                            contentDescriptionRes = R.string.save_to_home_screen,
                        )
                    },
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
        AndroidPetWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(appWidgetId))
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }
}

private fun NavigationSuiteScope.configNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    @StringRes contentDescriptionRes: Int = labelRes,
) {
    item(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = stringResource(id = contentDescriptionRes),
                modifier = Modifier.size(32.dp),
            )
        },
        label = { Text(text = stringResource(id = labelRes)) },
    )
}

@Composable
private fun BuddyConfigScreenContent(
    currentStep: ConfigStep,
    state: WidgetBuddyState,
    onStateChange: (WidgetBuddyState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val featureFilter = createEyeMouthColorFilter(MaterialTheme.colorScheme.onSurface.toArgb())

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
                DrawableOptionRow(
                    options = BuddyPreferences.BUDDY_OPTIONS,
                    selectedOption = state.buddyName,
                    onOptionSelected = { onStateChange(state.copy(buddyName = it)) },
                    drawableResFor = BuddyPreferences::getBodyDrawableResForBuddy,
                    optionModifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(28.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ConfigSectionCard(titleRes = R.string.choose_eyes_title) {
                DrawableOptionRow(
                    options = BuddyPreferences.EYE_OPTIONS,
                    selectedOption = state.eyes,
                    onOptionSelected = { onStateChange(state.copy(eyes = it)) },
                    drawableResFor = BuddyPreferences::getEyesDrawableRes,
                    optionModifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    horizontalPadding = 12.dp,
                    colorFilterFor = { eyeStyle ->
                        if (eyeStyle == "eyes_1" || eyeStyle == "eyes_5") featureFilter else null
                    },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ConfigSectionCard(
                titleRes = R.string.choose_mouth_title,
                subtitleRes = R.string.choose_mouth_subtitle,
                modifier = Modifier.padding(bottom = 2.dp),
            ) {
                DrawableOptionRow(
                    options = BuddyPreferences.MOUTH_OPTIONS,
                    selectedOption = state.mouth,
                    onOptionSelected = { onStateChange(state.copy(mouth = it)) },
                    drawableResFor = {
                        BuddyPreferences.getMouthDrawableRes(it, isGrownUp = true)
                            ?: R.drawable.mouth_1
                    },
                    optionModifier = Modifier.size(width = 72.dp, height = 52.dp),
                    shape = RoundedCornerShape(26.dp),
                    selectedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    contentScale = ContentScale.FillHeight,
                    colorFilterFor = { featureFilter },
                    imageModifierFor = { mouthStyle ->
                        val scale = if (mouthStyle == "mouth_1") 1.4f else 1.1f
                        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    },
                )
            }
        } else {
            ConfigSectionCard(
                titleRes = currentStep.titleRes,
                titleStyle = MaterialTheme.typography.headlineLarge,
                titleBottomPadding = 12.dp,
                shape = RoundedCornerShape(if (currentStep == ConfigStep.NAME) 24.dp else 48.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                when (currentStep) {
                    ConfigStep.NAME -> NameBuddyStepContent(
                        buddyNickname = state.nickname,
                        onNicknameChanged = {
                            onStateChange(state.copy(nickname = it.take(MAX_NICKNAME_LENGTH)))
                        },
                    )
                    ConfigStep.ENVIRONMENT -> DrawableOptionRow(
                        options = BuddyPreferences.ENVIRONMENT_OPTIONS,
                        selectedOption = state.environment,
                        onOptionSelected = { onStateChange(state.copy(environment = it)) },
                        drawableResFor = BuddyPreferences::getDrawableResForEnvironment,
                        optionModifier = Modifier.size(96.dp, 64.dp),
                        shape = RoundedCornerShape(24.dp),
                        verticalPadding = 12.dp,
                        contentPadding = PaddingValues(4.dp),
                        contentScale = ContentScale.Crop,
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
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    titleBottomPadding: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(48.dp),
    content: @Composable () -> Unit,
) {
    Text(
        text = stringResource(id = titleRes),
        style = titleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (subtitleRes == null) titleBottomPadding else 0.dp),
    )

    if (subtitleRes != null) {
        Text(
            text = stringResource(id = subtitleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
        content = content,
    )
}

@Composable
private fun NameBuddyStepContent(
    buddyNickname: String,
    onNicknameChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = buddyNickname,
        onValueChange = onNicknameChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
        label = { Text(stringResource(R.string.name_buddy_label)) },
        placeholder = { Text(stringResource(R.string.name_buddy_placeholder)) },
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
                text = "${buddyNickname.length} / $MAX_NICKNAME_LENGTH",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun BuddyConfigHeroPreviewCard(
    currentStep: ConfigStep,
    state: WidgetBuddyState,
    modifier: Modifier = Modifier,
) {
    val isShapeBg = currentStep == ConfigStep.BUDDY || currentStep == ConfigStep.NAME
    val bgRes = if (isShapeBg) {
        R.drawable.expressive_shape_2
    } else {
        BuddyPreferences.getDrawableResForEnvironment(state.environment)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = bgRes),
                contentDescription = null,
                modifier = if (isShapeBg) Modifier.size(260.dp) else Modifier.fillMaxSize(),
                contentScale = if (isShapeBg) ContentScale.Fit else ContentScale.Crop,
                colorFilter = if (isShapeBg) {
                    ColorFilter.tint(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    null
                },
            )
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
private fun SelectableOptionBox(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    selectedBackgroundColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(selectedBackgroundColor, shape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun DrawableOptionRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    drawableResFor: (String) -> Int,
    optionModifier: Modifier,
    shape: Shape,
    verticalPadding: Dp = 6.dp,
    horizontalPadding: Dp = 16.dp,
    selectedBackgroundColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(6.dp),
    contentScale: ContentScale = ContentScale.Fit,
    colorFilterFor: (String) -> ColorFilter? = { null },
    imageModifierFor: (String) -> Modifier = { Modifier },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding, horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            SelectableOptionBox(
                isSelected = selectedOption == option,
                onClick = { onOptionSelected(option) },
                modifier = optionModifier,
                shape = shape,
                selectedBackgroundColor = selectedBackgroundColor,
                contentPadding = contentPadding,
            ) {
                Image(
                    painter = painterResource(id = drawableResFor(option)),
                    contentDescription = option,
                    modifier = Modifier.fillMaxSize().then(imageModifierFor(option)),
                    contentScale = contentScale,
                    colorFilter = colorFilterFor(option),
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
    val scheme = MaterialTheme.colorScheme
    fun darker(a: Color, b: Color): Color = if (a.luminance() <= b.luminance()) a else b

    val originalBrush = Brush.sweepGradient(
        listOf(Color(0xFFFFA3E5), Color(0xFF80D8FF), Color(0xFFFFD180), Color(0xFFFFA3E5)),
    )
    val swatches = listOf(
        Triple(BuddyPreferences.DEFAULT_BUDDY_COLOR, BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR, Modifier.background(originalBrush)),
        Triple(BuddyPreferences.COLOR_ANDROID_GREEN, BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR, Modifier.background(Color(BuddyPreferences.COLOR_ANDROID_GREEN))),
        Triple(scheme.primary.toArgb(), darker(scheme.onPrimaryContainer, scheme.onPrimary).toArgb(), Modifier.background(scheme.primary)),
        Triple(scheme.secondary.toArgb(), darker(scheme.onSecondaryContainer, scheme.onSecondary).toArgb(), Modifier.background(scheme.secondary)),
        Triple(scheme.tertiary.toArgb(), darker(scheme.onTertiaryContainer, scheme.onTertiary).toArgb(), Modifier.background(scheme.tertiary)),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatches.forEach { (bodyArgb, eyeAccentArgb, bgModifier) ->
            SelectableOptionBox(
                isSelected = selectedThemeColor == bodyArgb,
                onClick = {
                    val eyeArgb = BuddyPreferences.resolveEyeMouthColor(bodyArgb, eyeAccentArgb)
                    onThemeAndEyeColorSelected(bodyArgb, eyeArgb)
                },
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(4.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).then(bgModifier))
            }
        }
    }
}
