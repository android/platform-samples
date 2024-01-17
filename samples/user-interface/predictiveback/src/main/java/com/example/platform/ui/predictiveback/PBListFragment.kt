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

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.example.platform.ui.predictiveback.databinding.FragmentAnimationListBinding

class PBListFragment : Fragment() {

    private var _binding: FragmentAnimationListBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAnimationListBinding.inflate(inflater, container, false)

        setAnimationText()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle insets so fragment is edge-to-edge
        binding.header.setOnApplyWindowInsetsListener { header, windowInsets ->
            val topBarInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            } else {
                windowInsets.systemWindowInsetTop
            }
            header.updatePadding(top = topBarInset)
            windowInsets
        }
        // TODO: This doesn't seem to apply a bottom inset to the last card
        binding.transitionsCard.setOnApplyWindowInsetsListener { card, windowInsets ->
            val bottomBarInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            } else {
                windowInsets.systemWindowInsetBottom
            }
            card.updatePadding(bottom = bottomBarInset)
            windowInsets
        }

        binding.customCrossActivityCard.setOnClickListener {
            findNavController().navigate(R.id.show_PBCustomCrossActivityAnimation)
        }
        binding.crossFragmentCard.setOnClickListener {
            findNavController().navigate(R.id.show_PBNavigationComponentDefaultAnimations)
        }
        // Create a predictive back cross fragment shared element transition
        binding.sharedElementCrossFragment.setOnClickListener {
            findNavController().navigate(
                R.id.show_PBSharedElementTransitionFragment,
                null,
                null,
                FragmentNavigatorExtras(it to "second_card")
            )
        }
        binding.progressApiCard.setOnClickListener {
            findNavController().navigate(R.id.show_PBProgressAPI)
        }
        binding.transitionsCard.setOnClickListener {
            findNavController().navigate(R.id.show_PBTransition)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setAnimationText() {
        binding.sysUiTitle.text = animations[PBAnimation.SYS_UI]?.title ?: ""
        binding.sysUiDescription.text = animations[PBAnimation.SYS_UI]?.description ?: ""
        binding.backToHomeTitle.text = animations[PBAnimation.BACK_TO_HOME]?.title ?: ""
        binding.backToHomeDescription.text = animations[PBAnimation.BACK_TO_HOME]?.description ?: ""
        binding.crossActivityTitle.text = animations[PBAnimation.CROSS_ACTIVITY]?.title ?: ""
        binding.crossActivityDescription.text = animations[PBAnimation.CROSS_ACTIVITY]?.description ?: ""
        binding.customCrossActivityTitle.text = animations[PBAnimation.CUSTOM_CROSS_ACTIVITY]?.title ?: ""
        binding.customCrossActivityDescription.text = animations[PBAnimation.CUSTOM_CROSS_ACTIVITY]?.description ?: ""
        binding.crossFragmentTitle.text = animations[PBAnimation.CROSS_FRAGMENT]?.title ?: ""
        binding.crossFragmentDescription.text = animations[PBAnimation.CROSS_FRAGMENT]?.description ?: ""
        binding.sharedElementCrossFragmentTitle.text = animations[PBAnimation.SHARED_ELEMENT_CROSS_FRAGMENT]?.title ?: ""
        binding.sharedElementCrossFragmentDescription.text = animations[PBAnimation.SHARED_ELEMENT_CROSS_FRAGMENT]?.description ?: ""
        binding.progressApiTitle.text = animations[PBAnimation.PROGRESS_API]?.title ?: ""
        binding.progressApiDescription.text = animations[PBAnimation.PROGRESS_API]?.description ?: ""
        binding.transitionsTitle.text = animations[PBAnimation.TRANSITION]?.title ?: ""
        binding.transitionsDescription.text = animations[PBAnimation.TRANSITION]?.description ?: ""
    }
}