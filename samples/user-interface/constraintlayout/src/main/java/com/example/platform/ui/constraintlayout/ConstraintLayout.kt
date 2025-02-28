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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.example.platform.ui.constraintlayout.databinding.ConstraintSetMainBinding

class CenteringViewsFragment : Fragment(R.layout.centering_views)

class BasicArrangementFragment : Fragment(R.layout.basic_arrangement)

class AdvancedArrangementFragment : Fragment(R.layout.advanced_arrangement)

class AspectRatioFragment : Fragment(R.layout.aspect_ratio)

class BasicChainFragment : Fragment(R.layout.basic_chains)

class AdvancedChainsFragment : Fragment(R.layout.advanced_chains)

class ConstraintSetFragment : Fragment(R.layout.constraint_set_main) {

    private lateinit var binding: ConstraintSetMainBinding

    private var showBigImage = false

    private val constraintSetNormal = ConstraintSet()
    private val constraintSetBig = ConstraintSet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = ConstraintSetMainBinding.bind(view)
        // Note that this can also be achieved by calling
        // `constraintSetNormal.load(this, R.layout.constraint_set_main);`
        // Since we already have an inflated ConstraintLayout in the layout file, clone() is
        // faster and considered the best practice.
        constraintSetNormal.clone(binding.root)
        // Load the constraints from the layout where ImageView is enlarged.
        constraintSetBig.load(requireContext(), R.layout.constraint_set_big)
        binding.image.setOnClickListener { toggle() }
    }

    private fun toggle() {
        showBigImage = !showBigImage
        if (showBigImage) {
            constraintSetBig.applyTo(binding.root)
        } else {
            constraintSetNormal.applyTo(binding.root)
        }
    }
}

class GuidelinesFragment : Fragment(R.layout.guidelines)
