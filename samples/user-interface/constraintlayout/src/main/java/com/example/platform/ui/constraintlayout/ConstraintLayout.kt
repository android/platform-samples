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
import com.google.android.catalog.framework.annotations.Sample

@Sample(
    name = "ConstraintLayout - 1. Centering Views",
    description = "Center child views horizontally or vertically.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
class CenteringViewsFragment : Fragment(R.layout.centering_views)

@Sample(
    name = "ConstraintLayout - 2. Basic arrangement",
    description = "Arrange positions of child views relative to other views.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
class BasicArrangementFragment : Fragment(R.layout.basic_arrangement)

@Sample(
    name = "ConstraintLayout - 3. Advanced arrangement",
    description = "More arrangement options.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
class AdvancedArrangementFragment : Fragment(R.layout.advanced_arrangement)

@Sample(
    name = "ConstraintLayout - 4. Aspect ratio",
    description = "Specify aspect ratio for the dimensions of the child views.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)

class AspectRatioFragment : Fragment(R.layout.aspect_ratio)

@Sample(
    name = "ConstraintLayout - 5. Basic chains",
    description = "Use chains to arrange multiple child views horizontally or vertically.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
class BasicChainFragment : Fragment(R.layout.basic_chains)

@Sample(
    name = "ConstraintLayout - 6. Advanced chains",
    description = "Use chains to arrange multiple child views horizontally or vertically.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
class AdvancedChainsFragment : Fragment(R.layout.advanced_chains)

@Sample(
    name = "ConstraintLayout - 7. ConstraintSet",
    description = "Use ConstraintSet to specify multiple constraints to all the child views.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
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

@Sample(
    name = "ConstraintLayout - 8. Guidelines",
    description = "Use a horizontal or vertical guideline to apply constraints to child views.",
    documentation = "https://developer.android.com/develop/ui/views/layout/constraint-layout",
    tags = ["constraint-layout"],
)
class GuidelinesFragment : Fragment(R.layout.guidelines)
