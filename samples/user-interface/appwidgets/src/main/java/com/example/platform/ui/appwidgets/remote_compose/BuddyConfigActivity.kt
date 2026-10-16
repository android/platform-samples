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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.platform.ui.appwidgets.R

/**
 * Configuration activity for [AndroidPetWidget], allowing the user to customize the pet's
 * body, eyes, mouth, nickname, room environment, and theme colors before placing the widget.
 */
class BuddyConfigActivity : ComponentActivity() {

    var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        private set
    var selectedBuddy by mutableStateOf(BuddyPreferences.DEFAULT_BUDDY)
        internal set
    var selectedEyes by mutableStateOf(BuddyPreferences.DEFAULT_EYES)
        internal set
    var selectedMouth by mutableStateOf(BuddyPreferences.DEFAULT_MOUTH)
        internal set
    var buddyNickname by mutableStateOf(BuddyPreferences.DEFAULT_BUDDY_NAME)
        internal set
    var selectedEnv by mutableStateOf(BuddyPreferences.DEFAULT_ENVIRONMENT)
        internal set
    var selectedThemeColor by mutableIntStateOf(BuddyPreferences.DEFAULT_BUDDY_COLOR)
        internal set
    var selectedEyeMouthColor by mutableIntStateOf(BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR)
        internal set
    var currentStep by mutableIntStateOf(STEP_BUDDY)
        internal set

    companion object {
        const val STEP_BUDDY = 1
        const val STEP_NAME = 2
        const val STEP_ENVIRONMENT = 3
        const val STEP_THEME = 4

        private const val KEY_SELECTED_BUDDY = "selected_buddy"
        private const val KEY_SELECTED_EYES = "selected_eyes"
        private const val KEY_SELECTED_MOUTH = "selected_mouth"
        private const val KEY_BUDDY_NICKNAME = "buddy_nickname"
        private const val KEY_SELECTED_ENV = "selected_env"
        private const val KEY_SELECTED_THEME_COLOR = "selected_theme_color"
        private const val KEY_SELECTED_EYE_MOUTH_COLOR = "selected_eye_mouth_color"
        private const val KEY_CURRENT_STEP = "current_step"
    }

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

        val state = BuddyPreferences.getWidgetBuddyState(this, appWidgetId)
        if (savedInstanceState != null) {
            selectedBuddy = savedInstanceState.getString(KEY_SELECTED_BUDDY, state.buddyName)
            selectedEyes = savedInstanceState.getString(KEY_SELECTED_EYES, state.eyes)
            selectedMouth = savedInstanceState.getString(KEY_SELECTED_MOUTH, state.mouth)
            buddyNickname = savedInstanceState.getString(KEY_BUDDY_NICKNAME, state.nickname)
            selectedEnv = savedInstanceState.getString(KEY_SELECTED_ENV, state.environment)
            selectedThemeColor = savedInstanceState.getInt(KEY_SELECTED_THEME_COLOR, state.buddyColor)
            selectedEyeMouthColor = savedInstanceState.getInt(KEY_SELECTED_EYE_MOUTH_COLOR, state.eyeMouthColor)
            currentStep = savedInstanceState.getInt(KEY_CURRENT_STEP, STEP_BUDDY)
        } else {
            selectedBuddy = state.buddyName
            selectedEyes = state.eyes
            selectedMouth = state.mouth
            buddyNickname = state.nickname
            selectedEnv = state.environment
            selectedThemeColor = state.buddyColor
            selectedEyeMouthColor = state.eyeMouthColor
        }

        setContent {
            StoreTheme(darkTheme = false) {
                val railItemColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF1B1C17),
                    indicatorColor = Color(0xFF50524A),
                    unselectedIconColor = Color(0xFF49454F),
                    unselectedTextColor = Color(0xFF49454F),
                )

                Scaffold(
                    containerColor = colorResource(id = R.color.config_bg),
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding),
                    ) {
                        NavigationRail(
                            containerColor = Color(0xFFEEF3E7),
                        ) {
                            NavigationRailItem(
                                selected = currentStep == STEP_BUDDY,
                                onClick = { showBuddySelection() },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_buddy),
                                        contentDescription = stringResource(id = R.string.nav_buddy),
                                        modifier = Modifier.size(32.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.nav_buddy),
                                        fontFamily = MadimiOneFontFamily,
                                    )
                                },
                                colors = railItemColors,
                            )
                            NavigationRailItem(
                                selected = currentStep == STEP_NAME,
                                onClick = { showNameBuddy() },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_name),
                                        contentDescription = stringResource(id = R.string.nav_name),
                                        modifier = Modifier.size(32.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.nav_name),
                                        fontFamily = MadimiOneFontFamily,
                                    )
                                },
                                colors = railItemColors,
                            )
                            NavigationRailItem(
                                selected = currentStep == STEP_ENVIRONMENT,
                                onClick = { showEnvironmentSelection() },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_environment),
                                        contentDescription = stringResource(id = R.string.nav_environment),
                                        modifier = Modifier.size(32.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.nav_environment),
                                        fontFamily = MadimiOneFontFamily,
                                    )
                                },
                                colors = railItemColors,
                            )
                            NavigationRailItem(
                                selected = currentStep == STEP_THEME,
                                onClick = { showThemeSelection() },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_palette),
                                        contentDescription = stringResource(id = R.string.nav_palette),
                                        modifier = Modifier.size(32.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.nav_palette),
                                        fontFamily = MadimiOneFontFamily,
                                    )
                                },
                                colors = railItemColors,
                            )
                            NavigationRailItem(
                                selected = false,
                                onClick = { commitSelection() },
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
                                colors = railItemColors,
                            )
                        }

                        BuddyConfigScreenContent(
                            currentStep = currentStep,
                            selectedBuddy = selectedBuddy,
                            selectedEyes = selectedEyes,
                            selectedMouth = selectedMouth,
                            buddyNickname = buddyNickname,
                            selectedEnv = selectedEnv,
                            selectedThemeColor = selectedThemeColor,
                            selectedEyeMouthColor = selectedEyeMouthColor,
                            onBuddySelected = { onBuddySelected(it) },
                            onEyesSelected = { onEyesSelected(it) },
                            onMouthSelected = { onMouthSelected(it) },
                            onNicknameChanged = { onNicknameChanged(it) },
                            onEnvironmentSelected = { onEnvironmentSelected(it) },
                            onThemeSelected = { onThemeSelected(it) },
                            onThemeAndEyeColorSelected = { color, eyeColor ->
                                onThemeSelected(color, eyeColor)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .imePadding(),
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_BUDDY, selectedBuddy)
        outState.putString(KEY_SELECTED_EYES, selectedEyes)
        outState.putString(KEY_SELECTED_MOUTH, selectedMouth)
        outState.putString(KEY_BUDDY_NICKNAME, buddyNickname)
        outState.putString(KEY_SELECTED_ENV, selectedEnv)
        outState.putInt(KEY_SELECTED_THEME_COLOR, selectedThemeColor)
        outState.putInt(KEY_SELECTED_EYE_MOUTH_COLOR, selectedEyeMouthColor)
        outState.putInt(KEY_CURRENT_STEP, currentStep)
    }

    internal fun onBuddySelected(buddyName: String) {
        selectedBuddy = buddyName
    }

    internal fun onEyesSelected(eyesStyle: String) {
        selectedEyes = eyesStyle
    }

    internal fun onMouthSelected(mouthStyle: String) {
        selectedMouth = mouthStyle
    }

    internal fun onNicknameChanged(name: String) {
        buddyNickname = name.take(20)
    }

    internal fun onEnvironmentSelected(envName: String) {
        selectedEnv = envName
    }

    internal fun onThemeSelected(
        colorArgb: Int,
        eyeMouthColorArgb: Int = BuddyPreferences.resolveEyeMouthColor(colorArgb),
    ) {
        selectedThemeColor = colorArgb
        selectedEyeMouthColor = eyeMouthColorArgb
    }

    internal fun showBuddySelection() {
        currentStep = STEP_BUDDY
    }

    internal fun showNameBuddy() {
        currentStep = STEP_NAME
    }

    internal fun showEnvironmentSelection() {
        currentStep = STEP_ENVIRONMENT
    }

    internal fun showThemeSelection() {
        currentStep = STEP_THEME
    }

    internal fun commitSelection() {
        val finalNickname = buddyNickname.trim().ifEmpty { BuddyPreferences.DEFAULT_BUDDY_NAME }
        BuddyPreferences.saveBuddyConfig(
            context = this,
            widgetId = appWidgetId,
            buddy = selectedBuddy,
            nickname = finalNickname,
            environment = selectedEnv,
            colorArgb = selectedThemeColor,
            eyes = selectedEyes,
            mouth = selectedMouth,
            eyeMouthColor = selectedEyeMouthColor,
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
    currentStep: Int,
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
    onThemeSelected: (Int) -> Unit = {},
    onThemeAndEyeColorSelected: (Int, Int) -> Unit = { color, _ -> onThemeSelected(color) },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        )

        if (currentStep == BuddyConfigActivity.STEP_BUDDY) {
            Text(
                text = stringResource(id = R.string.select_buddy_title),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MadimiOneFontFamily,
                    color = colorResource(id = R.color.title_text_color),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(48.dp),
                color = colorResource(id = R.color.selection_card_bg),
                shadowElevation = 4.dp,
            ) {
                BuddySelectionRow(
                    selectedBuddy = selectedBuddy,
                    onBuddySelected = onBuddySelected,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.choose_eyes_title),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MadimiOneFontFamily,
                    color = colorResource(id = R.color.title_text_color),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(48.dp),
                color = colorResource(id = R.color.selection_card_bg),
                shadowElevation = 4.dp,
            ) {
                EyesSelectionRow(
                    selectedEyes = selectedEyes,
                    onEyesSelected = onEyesSelected,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.choose_mouth_title),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MadimiOneFontFamily,
                    color = colorResource(id = R.color.title_text_color),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 2.dp),
                shape = RoundedCornerShape(48.dp),
                color = colorResource(id = R.color.selection_card_bg),
                shadowElevation = 4.dp,
            ) {
                MouthSelectionRow(
                    selectedMouth = selectedMouth,
                    onMouthSelected = onMouthSelected,
                )
            }
        } else {
            Text(
                text = stringResource(
                    id = when (currentStep) {
                        BuddyConfigActivity.STEP_NAME -> R.string.name_buddy_title
                        BuddyConfigActivity.STEP_ENVIRONMENT -> R.string.select_environment_title
                        else -> R.string.select_theme_title
                    },
                ),
                style = TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MadimiOneFontFamily,
                    color = colorResource(id = R.color.title_text_color),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(
                    if (currentStep == BuddyConfigActivity.STEP_NAME) 24.dp else 48.dp,
                ),
                color = colorResource(id = R.color.selection_card_bg),
                shadowElevation = 4.dp,
            ) {
                when (currentStep) {
                    BuddyConfigActivity.STEP_NAME -> NameBuddyStepContent(
                        buddyNickname = buddyNickname,
                        onNicknameChanged = onNicknameChanged,
                    )
                    BuddyConfigActivity.STEP_ENVIRONMENT -> EnvironmentSelectionRow(
                        selectedEnv = selectedEnv,
                        onEnvironmentSelected = onEnvironmentSelected,
                    )
                    else -> ThemeSelectionRow(
                        selectedThemeColor = selectedThemeColor,
                        onThemeSelected = onThemeSelected,
                        onThemeAndEyeColorSelected = onThemeAndEyeColorSelected,
                    )
                }
            }
        }
    }
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
    currentStep: Int,
    selectedBuddy: String,
    selectedEyes: String,
    selectedEnv: String,
    selectedThemeColor: Int,
    selectedEyeMouthColor: Int,
    modifier: Modifier = Modifier,
    selectedMouth: String = BuddyPreferences.DEFAULT_MOUTH,
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
            if (currentStep == BuddyConfigActivity.STEP_BUDDY || currentStep == BuddyConfigActivity.STEP_NAME) {
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

            val bodyColorFilter = if (
                selectedThemeColor != 0 && selectedThemeColor != BuddyPreferences.DEFAULT_BUDDY_COLOR
            ) {
                ColorFilter.tint(Color(selectedThemeColor), BlendMode.SrcIn)
            } else {
                null
            }
            val eyeMouthColorFilter = createEyeMouthColorFilter(selectedEyeMouthColor)
            val bodyPainter = painterResource(
                id = BuddyPreferences.getBodyDrawableResForBuddy(selectedBuddy),
            )
            BoxWithConstraints(
                modifier = Modifier.size(160.dp),
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
                    contentDescription = stringResource(id = R.string.preview_buddy_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = bodyColorFilter,
                )
                Image(
                    painter = painterResource(id = BuddyPreferences.getEyesDrawableRes(selectedEyes)),
                    contentDescription = null,
                    modifier = Modifier
                        .width(eyesWidth)
                        .offset(y = eyesOffsetY),
                    contentScale = ContentScale.FillWidth,
                    colorFilter = eyeMouthColorFilter,
                )
                Image(
                    painter = painterResource(id = BuddyPreferences.getMouthDrawableRes(selectedMouth)),
                    contentDescription = null,
                    modifier = Modifier
                        .width(mouthWidth)
                        .offset(y = mouthOffsetY),
                    contentScale = ContentScale.FillWidth,
                    colorFilter = eyeMouthColorFilter,
                )
            }
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
        BuddyOption(
            buddyName = "guy_bunny",
            isSelected = selectedBuddy == "guy_bunny",
            onClick = { onBuddySelected("guy_bunny") },
        )
        BuddyOption(
            buddyName = "guy_bun",
            isSelected = selectedBuddy == "guy_bun",
            onClick = { onBuddySelected("guy_bun") },
        )
        BuddyOption(
            buddyName = "guy_wiggle",
            isSelected = selectedBuddy == "guy_wiggle",
            onClick = { onBuddySelected("guy_wiggle") },
        )
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
            .padding(vertical = 6.dp, horizontal = 8.dp),
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
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
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
            val drawableRes = BuddyPreferences.getMouthDrawableRes(mouthStyle)
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
        EnvOption(
            envName = "env_room",
            drawableRes = R.drawable.env_room,
            isSelected = selectedEnv == "env_room",
            onClick = { onEnvironmentSelected("env_room") },
        )
        EnvOption(
            envName = "env_meadow",
            drawableRes = R.drawable.env_meadow,
            isSelected = selectedEnv == "env_meadow",
            onClick = { onEnvironmentSelected("env_meadow") },
        )
    }
}

@Composable
private fun ThemeSelectionRow(
    selectedThemeColor: Int,
    onThemeSelected: (Int) -> Unit,
    onThemeAndEyeColorSelected: (Int, Int) -> Unit = { color, _ -> onThemeSelected(color) },
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
            .padding(vertical = 12.dp, horizontal = 8.dp),
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
        ThemeOption(
            color = primaryColor,
            isSelected = selectedThemeColor == primaryColor.toArgb(),
            onClick = {
                val bodyArgb = primaryColor.toArgb()
                val eyeArgb = BuddyPreferences.resolveEyeMouthColor(bodyArgb, primaryEyeAccent.toArgb())
                onThemeAndEyeColorSelected(bodyArgb, eyeArgb)
            },
        )
        ThemeOption(
            color = secondaryColor,
            isSelected = selectedThemeColor == secondaryColor.toArgb(),
            onClick = {
                val bodyArgb = secondaryColor.toArgb()
                val eyeArgb = BuddyPreferences.resolveEyeMouthColor(bodyArgb, secondaryEyeAccent.toArgb())
                onThemeAndEyeColorSelected(bodyArgb, eyeArgb)
            },
        )
        ThemeOption(
            color = tertiaryColor,
            isSelected = selectedThemeColor == tertiaryColor.toArgb(),
            onClick = {
                val bodyArgb = tertiaryColor.toArgb()
                val eyeArgb = BuddyPreferences.resolveEyeMouthColor(bodyArgb, tertiaryEyeAccent.toArgb())
                onThemeAndEyeColorSelected(bodyArgb, eyeArgb)
            },
        )
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
            .size(40.dp)
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

/**
 * Creates a white-preserving [ColorFilter] that tints black (`#000000`) pupils, outlines, and mouths
 * to [eyeMouthColorArgb] while preserving white (`#FFFFFF`) eye sclera areas as pure white.
 */
internal fun createEyeMouthColorFilter(eyeMouthColorArgb: Int): ColorFilter {
    val targetColor = if (eyeMouthColorArgb == 0) {
        BuddyPreferences.DEFAULT_EYE_MOUTH_COLOR
    } else {
        eyeMouthColorArgb
    }
    val r = ((targetColor ushr 16) and 0xFF).toFloat()
    val g = ((targetColor ushr 8) and 0xFF).toFloat()
    val b = (targetColor and 0xFF).toFloat()
    val scaleR = 1f - (r / 255f)
    val scaleG = 1f - (g / 255f)
    val scaleB = 1f - (b / 255f)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                scaleR, 0f, 0f, 0f, r,
                0f, scaleG, 0f, 0f, g,
                0f, 0f, scaleB, 0f, b,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
