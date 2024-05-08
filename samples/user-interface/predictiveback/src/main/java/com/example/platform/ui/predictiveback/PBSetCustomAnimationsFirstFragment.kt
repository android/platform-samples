/*
 * Copyright 2024 The Android Open Source Project
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.platform.ui.predictiveback.databinding.FragmentSetCustomAnimationsBinding

class PBSetCustomAnimationsFirstFragment : Fragment() {

    private var _binding: FragmentSetCustomAnimationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSetCustomAnimationsBinding
            .inflate(inflater, container, false)

        binding.button.setOnClickListener {
            parentFragmentManager.commit {
                setCustomAnimations(
                    android.R.animator.fade_in, // enter
                    android.R.animator.fade_out, // exit
                    android.R.animator.fade_in, // popEnter
                    android.R.animator.fade_out) // popExit
                replace(R.id.fragment_container,PBSetCustomAnimationsSecondFragment())
                setReorderingAllowed(true)
                addToBackStack(null)
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}