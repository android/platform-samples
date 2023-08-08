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

package com.example.platform.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "Color Contrast",
    description = "This sample demonstrates the importance of proper color contrast and how to " +
            "audit your app to ensure proper color contrast.",
    documentation = "https://support.google.com/accessibility/android/answer/7158390"
)
@Composable
fun ColorContrast() {
    Column(
        Modifier.verticalScroll(rememberScrollState()),
    ) {
        TextWithPadding(
            "Sufficient color contrast benefits users with various visual impairments, but " +
                    "also helps all users when interacting with their device in extreme lighting" +
                    " conditions, such as in direct sunlight."
        )

        TextWithPadding(
            "Accessibility Scanner, available on the Play Store, can be used to detect color" +
                    " contrast issues. You can use Scanner on this screen to see how it" +
                    " identifies color contrast issues."
        )

        TextWithProperColorContrast(text = "This text has sufficient color contrast ratio")

        TextWithInsufficientColorContrast(
            text = "This text does not have sufficient color contrast ratio and should be " +
                    "identified by Scanner"
        )
    }
}

@Composable
fun TextWithProperColorContrast(text: String) {
    Text(text, color = Color.Blue, modifier = Modifier.padding(30.dp))
}

@Composable
fun TextWithInsufficientColorContrast(text: String) {
    Box(
        modifier = Modifier
            .background(Color.Gray)
            .padding(30.dp)
    ) {
        Text(text, color = Color.DarkGray)
    }
}
