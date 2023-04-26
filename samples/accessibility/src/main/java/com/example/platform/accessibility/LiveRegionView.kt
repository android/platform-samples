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


package com.example.platform.accessibility

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.fragment.app.Fragment
import com.example.platform.accessibility.databinding.LiveregionFragmentBinding
import com.google.android.catalog.framework.annotations.Sample
import java.util.concurrent.TimeUnit

@Sample(
    name = "Live Region (View)",
    description = "Utilize LiveRegion to automatically notify users of accessibility services" +
            " about changes to a view",
    documentation = "https://developer.android.com/reference/android/view/View#attr_android:accessibilityLiveRegion"
)
class LiveRegionView : Fragment(R.layout.liveregion_fragment) {

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = LiveregionFragmentBinding.bind(view)

        binding.introText.text = "Turn on TalkBack to navigate and interact with this screen. " +
                "Once TalkBack is turned on, move the accessibility focus to the Start Timer" +
                " button and activate it. The number in the middle of the screen will start" +
                " decrementing by 1 every second. Notice that even though the accessibility focus" +
                " remains on the button, TalkBack still announces updates to the timer view."

        val timer = object : CountDownTimer(
            TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(1)
        ) {
            override fun onTick(millisUntilFinished: Long) {
                // In liveregion_fragment.xml, timerView sets its accessibilityLiveRegion attribute
                // to "polite". This specifies to accessibility services such as TalkBack that
                // updates on this node should be announced even when it doesn't have accessibility
                // focus.
                binding.timerView.text =
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished).toString()
            }

            override fun onFinish() {}
        }

        binding.startButton.setOnClickListener {
            timer.start()
        }
    }
}
