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
import androidx.fragment.app.commitNow
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.platform.ui.constraintlayout.databinding.Motion16ViewpagerBinding
import com.example.platform.ui.constraintlayout.databinding.Motion20RevealBinding
import com.example.platform.ui.constraintlayout.databinding.Motion21ContainerBinding
import com.example.platform.ui.constraintlayout.databinding.Motion22ListFragmentBinding
import com.example.platform.ui.constraintlayout.databinding.Motion23ViewpagerBinding
import com.example.platform.ui.constraintlayout.databinding.Motion24YoutubeBinding
import com.example.platform.ui.constraintlayout.view.TextListAdapter
import com.example.platform.ui.constraintlayout.view.doOnTransitionCompleted
import com.example.platform.ui.constraintlayout.view.feedProgressTo
import com.google.android.material.tabs.TabLayoutMediator

class MotionBasic01Fragment : Fragment(R.layout.motion_01_basic)

class MotionBasic02Fragment : Fragment(R.layout.motion_02_basic)

class MotionBasic02NoAutoCompleteFragment : Fragment(R.layout.motion_02_basic_autocomplete_false)

class CustomAttributeFragment : Fragment(R.layout.motion_03_custom_attribute)

class ImageFilter1Fragment : Fragment(R.layout.motion_04_imagefilter)

class ImageFilter2Fragment : Fragment(R.layout.motion_05_imagefilter)

class KeyframePositionFragment : Fragment(R.layout.motion_06_keyframe)

class KeyframeInterpolationFragment : Fragment(R.layout.motion_07_keyframe)

class KeyframeCycleFragment : Fragment(R.layout.motion_08_cycle)

class Coordinator1Fragment : Fragment(R.layout.motion_09_coordinatorlayout)

class Coordinator2Fragment : Fragment(R.layout.motion_10_coordinatorlayout)

class Coordinator3Fragment : Fragment(R.layout.motion_11_coordinatorlayout)

class Drawer1Fragment : Fragment(R.layout.motion_12_drawerlayout)

class Drawer2Fragment : Fragment(R.layout.motion_13_drawerlayout)

class SidePanelFragment : Fragment(R.layout.motion_14_side_panel)

class ParallaxFragment : Fragment(R.layout.motion_15_parallax)

class ViewPagerFragment : Fragment(R.layout.motion_16_viewpager) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion16ViewpagerBinding.bind(view)
        val pages = listOf(
            R.layout.motion_16_viewpager_page1,
            R.layout.motion_16_viewpager_page2,
            R.layout.motion_16_viewpager_page3,
        )
        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = pages.size
            override fun createFragment(position: Int) = Fragment(pages[position])
        }
        binding.pager.feedProgressTo(binding.parallax.motionLayout)
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = "Page ${position + 1}"
        }.attach()
    }
}

class ComplexMotion1Fragment : Fragment(R.layout.motion_17_coordination)

class ComplexMotion2Fragment : Fragment(R.layout.motion_18_coordination)

class ComplexMotion3Fragment : Fragment(R.layout.motion_19_coordination)

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

class FragmentTransitionFragment : Fragment(R.layout.motion_21_container) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion21ContainerBinding.bind(view)
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                replace(R.id.container, FirstFragment())
            }
        }
        binding.motionLayout.doOnTransitionCompleted { currentId ->
            when (currentId) {
                R.id.start -> childFragmentManager.commitNow {
                    setCustomAnimations(0, R.animator.hide)
                    replace(R.id.container, FirstFragment())
                }

                R.id.end -> childFragmentManager.commitNow {
                    setCustomAnimations(R.animator.show, 0)
                    replace(R.id.container, SecondFragment())
                }
            }
        }
    }
}

class FirstFragment : Fragment(R.layout.motion_21_first_fragment)
class SecondFragment : Fragment(R.layout.motion_21_second_fragment)

class FragmentTransition2Fragment : Fragment(R.layout.motion_21_container) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion21ContainerBinding.bind(view)
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                replace(R.id.container, FirstFragment())
            }
        }
        binding.motionLayout.doOnTransitionCompleted { currentId ->
            when (currentId) {
                R.id.start -> childFragmentManager.commitNow {
                    setCustomAnimations(0, R.animator.hide)
                    replace(R.id.container, FirstFragment())
                }

                R.id.end -> childFragmentManager.commitNow {
                    setCustomAnimations(R.animator.show, 0)
                    replace(R.id.container, ListFragment())
                }
            }
        }
    }
}

class ListFragment : Fragment(R.layout.motion_22_list_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion22ListFragmentBinding.bind(view)
        binding.list.adapter = TextListAdapter(
            view.context.resources.getStringArray(R.array.animals).asList(),
        )
    }
}

class LottieFragment : Fragment(R.layout.motion_23_viewpager) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion23ViewpagerBinding.bind(view)
        val pages = listOf(
            R.layout.motion_16_viewpager_page1,
            R.layout.motion_16_viewpager_page2,
            R.layout.motion_16_viewpager_page3,
        )
        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = pages.size
            override fun createFragment(position: Int) = Fragment(pages[position])
        }
        binding.pager.feedProgressTo(binding.parallax.motionLayout)
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = "Page ${position + 1}"
        }.attach()
    }
}

class YoutubeFragment : Fragment(R.layout.motion_24_youtube) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = Motion24YoutubeBinding.bind(view)
        binding.recyclerviewFront.adapter = TextListAdapter(
            view.context.resources.getStringArray(R.array.animals).asList(),
        )
    }
}

class KeyTriggerFragment : Fragment(R.layout.motion_25_keytrigger)

class MultiStateFragment : Fragment(R.layout.motion_26_multistate)
