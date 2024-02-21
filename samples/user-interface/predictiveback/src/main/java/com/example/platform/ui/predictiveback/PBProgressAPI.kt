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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import com.example.platform.ui.predictiveback.databinding.FragmentProgressApiBinding

class PBProgressAPI : Fragment() {
    companion object {
        val STANDARD_DECELERATE: Interpolator = PathInterpolator(0f, 0f, 0f, 1f)
    }
    private var _binding: FragmentProgressApiBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProgressApiBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val windowWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().windowManager.currentWindowMetrics.bounds.width()
        } else {
            0
        }
        val maxXShift = windowWidth / 20

        val predictiveBackCallback = object: OnBackPressedCallback(enabled=false) {

            // Follows Predictive Back Design Guidance: https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                // Applies a decelerate interpolator
                val backProgress = backEvent.progress
                val interpolatedProgress = STANDARD_DECELERATE.getInterpolation(backProgress)

                when (backEvent.swipeEdge) {
                    BackEventCompat.EDGE_LEFT ->
                        binding.box.translationX = interpolatedProgress * maxXShift
                    BackEventCompat.EDGE_RIGHT ->
                        binding.box.translationX = -(interpolatedProgress * maxXShift)
                }
                binding.box.scaleX = 1F - (0.1F * interpolatedProgress)
                binding.box.scaleY = 1F - (0.1F * interpolatedProgress)
            }

            override fun handleOnBackPressed() {
                Toast.makeText(
                    context,
                    "Disable the callback to go back.",
                    Toast.LENGTH_SHORT
                ).show()
                resetCallbackState()
            }

            override fun handleOnBackCancelled() {
                resetCallbackState()
            }

            private fun resetCallbackState() {
                binding.box.scaleX = 1F
                binding.box.scaleY = 1F
                binding.box.translationX = 0F
            }

        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,
            predictiveBackCallback
        )
        updateCallbackState(predictiveBackCallback)

        binding.button.setOnClickListener {
            predictiveBackCallback.isEnabled = !predictiveBackCallback.isEnabled
            updateCallbackState(predictiveBackCallback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateCallbackState(callback: OnBackPressedCallback) {
        when (callback.isEnabled) {
            true -> {
                binding.button.text = "disable callback"
                binding.body.text =
                    "Swipe back to see the box move using the Predictive Back" +
                            " Progress APIs on Android 14+ devices."
            }
            false -> {
                binding.button.text = "enable callback"
                binding.body.text =
                    "Enable the callback so that it can intercept your back gestures."
            }
        }
    }
}