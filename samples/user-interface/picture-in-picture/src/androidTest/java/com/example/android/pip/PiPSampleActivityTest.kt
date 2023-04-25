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

package com.example.android.pip

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PiPSampleActivityTest {

    @Test
    fun start() {
        launchActivity<PiPSampleActivity>().use {
            onView(withId(R.id.time)).check(matches(withText("00:00:00")))
            onView(withId(R.id.start_or_pause)).perform(click())
            onView(withId(R.id.time)).check(matches(not(withText("00:00:00"))))
        }
    }

    @Test
    fun pip() {
        launchActivity<PiPSampleActivity>().use { scenario ->
            scenario.onActivity { activity ->
                assertThat(activity.isInPictureInPictureMode).isFalse()
            }
            onView(withId(R.id.pip)).perform(click())
            scenario.onActivity { activity ->
                assertThat(activity.isInPictureInPictureMode).isTrue()
            }
        }
    }
}
