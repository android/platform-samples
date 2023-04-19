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

package com.example.platform.app

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.espresso.Espresso
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests all the samples are present and open.
 *
 * Note: consider changing the test to use the TestNavController to control navigation instead
 */
@HiltAndroidTest
class NavigationTest {

    /**
     * Manages the components' state and is used to perform injection on your test
     */
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /**
     * Use the primary activity to initialize the app normally.
     */
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testSamplesOpen() {
        val searchLabel = "Search button"
        composeTestRule.apply {
            val platformLabel = onNodeWithText(activity.getString(R.string.app_name))
            val searchButton = onNodeWithContentDescription(searchLabel)
            platformLabel.assertIsDisplayed()
            searchButton.performClick()
            activity.catalogSamples.forEach {
                if (Build.VERSION.SDK_INT >= it.minSDK) {
                    onNode(hasImeAction(ImeAction.Search)).performTextInput(it.name)
                    onAllNodesWithText(it.name)
                        .filterToOne(hasImeAction(ImeAction.Search).not() and hasText(it.description))
                        .assertIsDisplayed()
                        .performClick()
                    Espresso.pressBack()
                    platformLabel.assertIsDisplayed()
                    searchButton.performClick()
                }
            }
        }
    }
}
