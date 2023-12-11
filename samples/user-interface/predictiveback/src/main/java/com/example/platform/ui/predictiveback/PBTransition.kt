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

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSeekController
import androidx.transition.TransitionSet
import com.example.platform.ui.predictiveback.databinding.FragmentTransitionBinding

class PBTransition : Fragment() {
    private var _binding: FragmentTransitionBinding? = null
    private val binding get() = _binding!!

    val transitionSet = TransitionSet().apply {
        addTransition(Fade(Fade.MODE_OUT))
        addTransition(ChangeBounds())
        addTransition(Fade(Fade.MODE_IN))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTransitionBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        changeTextVisibility(ShowText.SHORT)

        val callback = object : OnBackPressedCallback(enabled = false) {

            var controller: TransitionSeekController? = null

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                controller = TransitionManager.controlDelayedTransition(
                    binding.textContainer,
                    transitionSet
                )
                changeTextVisibility(ShowText.SHORT)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (controller?.isReady == true) {
                    controller?.currentFraction = backEvent.progress
                }
            }

            override fun handleOnBackPressed() {
                controller?.animateToEnd()
                this.isEnabled = false
            }

            override fun handleOnBackCancelled() {
                // If the user cancels the back gesture, reset the state
                TransitionManager.beginDelayedTransition(
                    binding.textContainer,
                    ChangeBounds()
                )
                changeTextVisibility(ShowText.LONG)
            }
        }

        binding.shortText.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.textContainer, transitionSet)
            changeTextVisibility(ShowText.LONG)
            callback.isEnabled = true
        }

        this.requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.longText.movementMethod = ScrollingMovementMethod()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class ShowText { SHORT, LONG }
    private fun changeTextVisibility(showText: ShowText) {
        when (showText) {
            ShowText.SHORT -> {
                binding.shortText.isVisible = true
                binding.longText.isVisible = false
                binding.body.text = "Click on the box."
            }
            ShowText.LONG -> {
                binding.shortText.isVisible = false
                binding.longText.isVisible = true
                binding.body.text = "Swipe back slowly to see the Predictive Back AndroidX Transition."
            }
        }
    }
}