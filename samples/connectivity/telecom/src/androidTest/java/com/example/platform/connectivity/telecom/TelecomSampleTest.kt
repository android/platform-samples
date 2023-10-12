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

package com.example.platform.connectivity.telecom

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests all the samples are present and open.
 *
 * Note: consider changing the test to use the TestNavController to control navigation instead
 */
class TelecomSampleTest {

    /**
     * Use the primary activity to initialize the app normally.
     */
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        listOf(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Avoids showing permission dialog when running certain samples
     */
    @get:Rule(order = 2)
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*permissionArray.toTypedArray())

    @Before
    fun setUp() {
        composeTestRule.setContent {
            TelecomCallSample()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOngoingCall() {
        composeTestRule.onNodeWithText("Make fake call").performClick()
        composeTestRule.apply {
            val isTablet = activity.isDeviceTablet()
            Log.d("TelecomSampleTest", "Testing on a tablet? $isTablet")

            // Wait till the call is connected
            waitUntilExactlyOneExists(hasText("Connected"), 5000)
            onNode(hasText("Bob")).assertIsDisplayed()

            val onHold = "Pause or resume call"
            onNodeWithContentDescription(onHold).apply {
                assertIsEnabled()
                assert(isOff())
                performClick()
            }
            waitUntilExactlyOneExists(hasContentDescription(onHold) and isOn(), 5000)

            // Disconnect call and check
            onNodeWithContentDescription("Disconnect call").performClick()
            waitUntil {
                onAllNodesWithText("Call ended").fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun Activity.isDeviceTablet() =
        !packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE) &&
                resources.configuration.smallestScreenWidthDp >= 600
}
