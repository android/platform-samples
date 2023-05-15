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

package com.example.platform.ui.constraintlayout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.platform.ui.constraintlayout.databinding.Motion20RevealBinding
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "MotionLayout - 01. Basic",
    description = "Basic motion example using referenced ConstraintLayout files",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class MotionBasic01Fragment : Fragment(R.layout.motion_01_basic)

@Sample(
    name = "MotionLayout - 02. Basic",
    description = "Basic motion example using ConstraintSets defined in the MotionScene file",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class MotionBasic02Fragment : Fragment(R.layout.motion_02_basic)

@Sample(
    name = "MotionLayout - 02. Basic, no auto complete",
    description = "Basic motion example same as 2, but autoComplete is set to false in onSwipe",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class MotionBasic02NoAutoCompleteFragment : Fragment(R.layout.motion_02_basic_autocomplete_false)

@Sample(
    name = "MotionLayout - 03. Custom attribute",
    description = "Show color interpolation (custom attribute)",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class CustomAttributeFragment : Fragment(R.layout.motion_03_custom_attribute)

@Sample(
    name = "MotionLayout - 04. ImageFilterView 1",
    description = "Show image cross-fade (using ML's ImageFilterView + custom attribute)",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ImageFilter1Fragment : Fragment(R.layout.motion_04_imagefilter)

@Sample(
    name = "MotionLayout - 05. ImageFilterView 2",
    description = "Show image saturation transition (using ML's ImageFilterView + custom attribute)",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ImageFilter2Fragment : Fragment(R.layout.motion_05_imagefilter)

@Sample(
    name = "MotionLayout - 06. Keyframe",
    description = "Use a simple keyframe to change the interpolated motion",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class KeyframePositionFragment : Fragment(R.layout.motion_06_keyframe)

@Sample(
    name = "MotionLayout - 07. Keyframe",
    description = "More complex keyframe, adding rotation interpolation",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class KeyframeInterpolationFragment : Fragment(R.layout.motion_07_keyframe)

@Sample(
    name = "MotionLayout - 08. Keyframe",
    description = "Basic example of using a keyframe cycle",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class KeyframeCycleFragment : Fragment(R.layout.motion_08_cycle)

@Sample(
    name = "MotionLayout - 09. CoordinatorLayout 1",
    description = "Basic example of using MotionLayout instead of AppBarLayout",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class Coordinator1Fragment : Fragment(R.layout.motion_09_coordinatorlayout)

@Sample(
    name = "MotionLayout - 10. CoordinatorLayout 2",
    description = "Slightly more complex example of MotionLayout replacing AppBarLayout, with multiple elements and parallax background",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class Coordinator2Fragment : Fragment(R.layout.motion_10_coordinatorlayout)

@Sample(
    name = "MotionLayout - 11. CoordinatorLayout 3",
    description = "Another AppBarLayout replacement example",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class Coordinator3Fragment : Fragment(R.layout.motion_11_coordinatorlayout)

@Sample(
    name = "MotionLayout - 12. DrawerLayout 1",
    description = "Basic DrawerLayout with MotionLayout",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class Drawer1Fragment : Fragment(R.layout.motion_12_drawerlayout)

@Sample(
    name = "MotionLayout - 13. DrawerLayout 2",
    description = "Advanced DrawerLayout with MotionLayout",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class Drawer2Fragment : Fragment(R.layout.motion_13_drawerlayout)

@Sample(
    name = "MotionLayout - 14. SidePanel",
    description = "Side Panel, implemented with MotionLayout only",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class SidePanelFragment : Fragment(R.layout.motion_14_side_panel)

@Sample(
    name = "MotionLayout - 15. Parallax",
    description = "Parallax background. Drag the car.",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ParallaxFragment : Fragment(R.layout.motion_15_parallax)

// TODO
/*
@Sample(
    name = "MotionLayout - 16. ViewPager",
    description = "Using MotionLayout with ViewPager",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
 */
class ViewPagerFragment : Fragment(R.layout.motion_16_viewpager)

@Sample(
    name = "MotionLayout - 17. Complex Motion 1",
    description = "Basic CoordinatorLayout-like behavior. Implemented with MotionLayout only, using a moving guideline. Note the view isn't resized.",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ComplexMotion1Fragment : Fragment(R.layout.motion_17_coordination)

@Sample(
    name = "MotionLayout - 18. Complex Motion 2",
    description = "Advanced CoordinatorLayout-like behavior (adding a FAB). Implemented with MotionLayout only, using a moving guideline. Note the view isn't resized.",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ComplexMotion2Fragment : Fragment(R.layout.motion_18_coordination)

@Sample(
    name = "MotionLayout - 19. Complex Motion 3",
    description = "Advanced CoordinatorLayout-like behavior (adding a FAB). Implemented with MotionLayout only, using direct resizing of the view.",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ComplexMotion3Fragment : Fragment(R.layout.motion_19_coordination)

@Sample(
    name = "MotionLayout - 20. Complex Motion 4",
    description = "Advanced Synchronized reveal motion + helper (bounce). Implemented with MotionLayout only.",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class ComplexMotion4Fragment : Fragment(R.layout.motion_20_reveal) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion20RevealBinding.bind(view)
        binding.info.setOnClickListener {
            if (binding.motionLayout.progress > 0.5f) {
                binding.motionLayout.transitionToStart()
            } else {
                binding.motionLayout.transitionToEnd()
            }
        }
    }
    }

/*
@Sample(
    name = "MotionLayout - 21. Fragment transition 1",
    description = "Using MotionLayout with ViewPager",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
 */
class FragmentTransitionFragment : Fragment(R.layout.motion_21_main_fragment)

/*
@Sample(
    name = "MotionLayout - 22. Fragment transition 2",
    description = "Using MotionLayout with ViewPager",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
 */
class FragmentTransition2Fragment : Fragment(R.layout.motion_22_list_fragment)

/*
@Sample(
    name = "MotionLayout - 23. Lottie",
    description = "Using MotionLayout and Lottie with ViewPager",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
 */
class LottieFragment : Fragment(R.layout.motion_23_parallax)

/*
@Sample(
    name = "MotionLayout - 24. YouTube-like motion",
    description = "Example showing a transition like YouTube",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
 */
class YoutubeFragment : Fragment(R.layout.motion_24_youtube)

@Sample(
    name = "MotionLayout - 25. KeyTrigger",
    description = "Example that calls a method using KeyTrigger",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class KeyTriggerFragment : Fragment(R.layout.motion_25_keytrigger)

@Sample(
    name = "MotionLayout - 26. Multi-state",
    description = "Example that transitions between multiple states",
    documentation = "https://developer.android.com/develop/ui/views/animations/motionlayout",
)
class MultiStateFragment : Fragment(R.layout.motion_26_multistate)
