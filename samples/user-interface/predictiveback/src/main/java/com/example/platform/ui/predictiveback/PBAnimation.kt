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

package com.example.platform.ui.predictiveback

class PBAnimation(val title: String, val description: String)

val pbAnimations = mutableListOf<PBAnimation>().apply {
    add(
        PBAnimation(
            "Enable System Animations",
            "The system animations are back-to-home and default cross-activity. To see them: (1) Enable gesture navigation. (2) In Android 14, enable the Predictive Back Developer Option."
        )
    )
    add(
        PBAnimation(
            "Back-to-Home",
            "To see the back-to-home animation, go to the root activity and swipe back from either the left or right edge to exit the app."
        )
    )
    add(
        PBAnimation(
            "Default Cross-Activity",
            "To see the default cross-activity animation in this app, on this screen swipe back from either the left or right edge."
        )
    )
    add(
        PBAnimation(
            "Cross-Fragment",
            "Animation example coming soon."
        )
    )
}