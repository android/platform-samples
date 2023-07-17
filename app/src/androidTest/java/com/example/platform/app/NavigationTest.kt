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

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.test.espresso.Espresso
import androidx.test.rule.GrantPermissionRule
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

    /**
     * Avoids showing permission dialog when running certain samples
     */
    @get:Rule(order = 3)
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
    )

    @Test
    fun testSamplesOpen() {
        composeTestRule.apply {
            val platformLabel = onNodeWithText(activity.getString(R.string.app_name))
            val scrollNode = onAllNodes(hasScrollAction() and hasScrollToIndexAction()).onFirst()

            // For each sample find it in the list, open it and go back
            activity.catalogSamples.forEach {
                // Skip disabled samples
                if (Build.VERSION.SDK_INT >= it.minSDK) {
                    try {
                        scrollNode.performScrollToKey(it.route)
                        onNode(hasText(it.name) and hasText(it.description)).performClick()

                        // Go back
                        Espresso.pressBack()
                        platformLabel.assertIsDisplayed()
                    } catch (e: Exception) {
                        throw Exception("Test failed in sample ${it.name}", e)
                    }
                }
            }
        }
    }
}
