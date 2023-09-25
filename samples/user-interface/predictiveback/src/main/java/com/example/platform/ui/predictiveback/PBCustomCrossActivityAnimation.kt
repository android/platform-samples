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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.platform.ui.predictiveback.databinding.ActivityCustomCrossAnimationBinding

class PBCustomCrossActivityAnimation : AppCompatActivity() {

    private lateinit var binding: ActivityCustomCrossAnimationBinding

    @RequiresApi(34)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCustomCrossAnimationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overrideActivityTransition(
            OVERRIDE_TRANSITION_OPEN,
            android.R.anim.fade_in,
            0
        )


        overrideActivityTransition(
            OVERRIDE_TRANSITION_CLOSE,
            0,
            android.R.anim.fade_out
        )
    }
}