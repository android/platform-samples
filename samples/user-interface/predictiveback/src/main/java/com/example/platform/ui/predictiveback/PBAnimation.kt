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

enum class PBAnimation {
    SYS_UI,
    BACK_TO_HOME,
    CROSS_ACTIVITY,
    CUSTOM_CROSS_ACTIVITY,
    CROSS_FRAGMENT,
    SHARED_ELEMENT_CROSS_FRAGMENT,
    PROGRESS_API,
    TRANSITION,
    MATERIAL_SHARED_AXIS,
    SET_CUSTOM_ANIMATIONS
}
data class PBAnimationText(val title: String, val description: String)

val animations = mapOf<PBAnimation, PBAnimationText>(
    PBAnimation.SYS_UI to PBAnimationText(
        "Enable System Animations",
        "The system animations are back-to-home and default cross-activity. To see them: (1) Enable gesture navigation. (2) In Android 14, enable the Predictive Back Developer Option."
    ),
    PBAnimation.BACK_TO_HOME to PBAnimationText(
        "Back-to-Home",
        "To see the back-to-home animation, go to the root activity and swipe back from either the left or right edge to exit the app."
    ),
    PBAnimation.CROSS_ACTIVITY to PBAnimationText(
        "Default Cross-Activity",
        "To see the default cross-activity animation, on this screen swipe back from either the left or right edge."
    ),
    PBAnimation.CUSTOM_CROSS_ACTIVITY to PBAnimationText(
        "Custom Cross-Activity",
        "Click to see a custom cross-activity animation."
    ),
    PBAnimation.CROSS_FRAGMENT to PBAnimationText(
        "Cross-Fragment",
        "Click to see cross-fragment animations with Navigation Component's default animators."
    ),
    PBAnimation.SHARED_ELEMENT_CROSS_FRAGMENT to PBAnimationText(
        title = "Shared Element Cross-Fragment",
        description = "Click to see a shared element cross-fragment animation."
    ),
    PBAnimation.PROGRESS_API to PBAnimationText(
        "Progress API",
        "Click to see an animation created with the Predictive Back Progress API."
    ),
    PBAnimation.TRANSITION to PBAnimationText(
        "Transition",
        "Click to see an animation created with AndroidX Transitions and the Predictive Back Progress API."
    ),
    PBAnimation.MATERIAL_SHARED_AXIS to PBAnimationText(
        "Material Shared Axis",
        "Click to see an animation created with Material Shared Axis."
    ),
    PBAnimation.SET_CUSTOM_ANIMATIONS to PBAnimationText(
        "setCustomAnimations",
        "Click to see an animation created with setCustomAnimations."
    )

)